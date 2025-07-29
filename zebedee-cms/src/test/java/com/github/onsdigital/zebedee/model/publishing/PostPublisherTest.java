package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.service.KafkaService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PostPublisherTest {

    @Mock
    private Collection collection;

    @Mock
    private Zebedee zebedee;

    @Mock
    private CollectionReader collectionReader;

    @Mock
    private KafkaService kafkaService;

    @BeforeClass
    public static void enableKafkaForTests() {
        System.setProperty("ENABLE_KAFKA", "true");
        CMSFeatureFlags.reset(); // forces reload of instance with updated config
    }

    @AfterClass
    public static void disableKafkaForTests() {
        System.clearProperty("ENABLE_KAFKA");
        CMSFeatureFlags.reset();
    }

    @Before
    public void setup() throws Exception {
        MDC.clear();
        setKafkaServiceSupplier(kafkaService);
    }

    @Test
    public void isIndexedUriShouldBeTrueIfNotVersioned() {

        // Given a URI that is not that of a versioned file
        String uri = "/some/unversioned/uri";

        // When the isIndexedUri method is called
        boolean isIndexed = PostPublisher.isIndexedUri(uri);

        // Then the result should be true, ie, it should be indexed.
        assertTrue(isIndexed);
    }

    @Test
    public void isIndexedUriShouldBeFalseIfVersioned() {

        // Given a URI that is that of a versioned file
        String uri = "/some/versioned/uri/previous/v1";

        // When the isIndexedUri method is called
        boolean isIndexed = PostPublisher.isIndexedUri(uri);

        // Then the result should be false, ie, it should not be indexed.
        assertFalse(isIndexed);
    }

    @Test
    public void testConvertUriWithJsonForEvent() {

        //Given {a single uri with data.json ready to be sent to kafka}
        String testUri = "/testUri0/data.json";

        //When {sending a uris to kafka}
        String actual = PostPublisher.convertUriForEvent(testUri);

        System.out.println(actual);

        //Then {uri does not have "data.json" string}
        assertFalse(actual.contains("/testUri0/data.json"));
        Assert.assertTrue(actual.contains("/testUri0"));
    }

    @Test
    public void testConvertUriWithOutJsonForEvent() {

        //Given {a single uri without data.json ready to be sent to kafka}
        String testUri1 = "/testUri1";

        //When {sending a uri to kafka}
        String actual = PostPublisher.convertUriForEvent(testUri1);

        //Then {uris returns the original string}
        Assert.assertTrue(actual.contains("/testUri1"));
    }

    @Test
    public void testConvertUriForEventWithEmptyString() {
        String uri = "";
        String result = PostPublisher.convertUriForEvent(uri);
        assertEquals("", result);
    }

    @Test
    public void testSendMessageSendsToKafkaSuccessfully() throws IOException {
        List<String> uris = Arrays.asList("/uri1", "/uri2");
        when(collection.getId()).thenReturn("col123");
        MDC.put("trace_id", "trace-abc");

        invokeSendMessage(collection, uris, "datasets");

        verify(kafkaService, times(1))
                .produceContentUpdated("col123", uris, "datasets", "", "ONS", "trace-abc");
    }

    @Test
    public void testSendContentUpdatedEventsWithEmptyDatasetVersionDetails() throws Exception {
        Collection mockCollection = mock(Collection.class);
        when(mockCollection.getDatasetVersionDetails()).thenReturn(Collections.emptyList());

        Content mockReviewed = mock(Content.class);
        when(mockCollection.getReviewed()).thenReturn(mockReviewed);
        when(mockReviewed.uris()).thenReturn(Collections.emptyList());

        Method method = PostPublisher.class.getDeclaredMethod("sendContentUpdatedEvents", Collection.class);
        method.setAccessible(true);
        method.invoke(null, mockCollection);

        verify(kafkaService).produceContentUpdated(any(), eq(Collections.emptyList()), eq("legacy"), any(), eq("ONS"), any());
    }


    @Test
    public void testCopyFilesToMasterSkipsZipAndVersionedFiles() throws Exception {
        Content publishedContent = mock(Content.class);
        Path tempDir = Files.createTempDirectory("published");
        when(zebedee.getPublished()).thenReturn(publishedContent);

        when(publishedContent.toPath("/some/content"))
                .thenReturn(tempDir.resolve("some/content"));

        Content reviewedContent = mock(Content.class);
        List<String> uris = Arrays.asList(
                "/some/content",
                "/some/versioned/uri/previous/v1",
                "/some/zip/timeseries-to-publish.zip"
        );
        when(reviewedContent.uris()).thenReturn(uris);
        when(collection.getReviewed()).thenReturn(reviewedContent);

        Resource resource = mock(Resource.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        when(collectionReader.getResource("/some/content")).thenReturn(resource);
        when(resource.getData()).thenReturn(stream);

        invokeCopyFilesToMaster(zebedee, collection, collectionReader);

        verify(collectionReader, times(1)).getResource("/some/content");
        verify(resource, times(1)).getData();
        verify(collectionReader, never()).getResource("/some/versioned/uri/previous/v1");
        verify(collectionReader, never()).getResource("/some/zip/timeseries-to-publish.zip");

        File expected = tempDir.resolve("some/content").toFile();
        assertTrue("Expected file should exist", expected.exists());
    }

    @Test
    public void testGetPublishedCollectionReadsJson() throws Exception {
        Path tempDir = Files.createTempDirectory("collection");
        File jsonFile = tempDir.resolve("collection123.json").toFile();
        Files.write(jsonFile.toPath(), "{\"name\":\"Test Collection\"}".getBytes());

        when(collection.getPath()).thenReturn(tempDir.resolve("collection123"));

        PostPublisher.getPublishedCollection(collection); // Should not throw
    }

    @Test
    public void testSendContentDeletedEventsToKafkaSuccess() throws Exception {
        List<String> uris = Arrays.asList("/to/delete");
        when(collection.getId()).thenReturn("col123");
        MDC.put("trace_id", "trace-xyz");

        Method method = PostPublisher.class.getDeclaredMethod("sendContentDeletedEventsToKafka", Collection.class, List.class);
        method.setAccessible(true);
        method.invoke(null, collection, uris);

        verify(kafkaService).produceContentDeleted("col123", uris, "ONS", "trace-xyz");
    }

    @Test
    public void testApplyManifestDeletesToMasterDeletesExpectedDirectory() throws Exception {
        // Setup temp collection folder
        Path tempDir = Files.createTempDirectory("collection");
        Path collectionPath = tempDir.resolve("testCollection");
        Files.createDirectories(collectionPath);

        // Create a directory that should be deleted
        Path toDeleteDir = tempDir.resolve("publishing/some/deleted/uri");
        Files.createDirectories(toDeleteDir);
        assertTrue("Setup: directory should exist before delete", Files.exists(toDeleteDir));

        // Write manifest.json to collection path
        Path manifestFile = collectionPath.resolve("manifest.json");
        String manifestContent = "{\"urisToDelete\":[\"/some/deleted/uri\"],\"filesToCopy\":[]}";
        Files.write(manifestFile, manifestContent.getBytes());

        // Mock collection
        when(collection.getPath()).thenReturn(collectionPath);

        // Create ContentReader mock that resolves target path
        ContentReader mockContentReader = mock(ContentReader.class);
        Path publishingRoot = tempDir.resolve("publishing");
        when(mockContentReader.getRootFolder()).thenReturn(publishingRoot);

        // ContentWriter not used here
        ContentWriter mockContentWriter = mock(ContentWriter.class);

        // Invoke method
        Method method = PostPublisher.class.getDeclaredMethod("applyManifestDeletesToMaster", Collection.class, ContentReader.class, ContentWriter.class);
        method.setAccessible(true);
        method.invoke(null, collection, mockContentReader, mockContentWriter);

        // Assert the directory was deleted
        assertFalse("Directory should have been deleted", Files.exists(toDeleteDir));
    }

    @Test
    public void testProcessManifestForMasterDeletesAndCopiesFiles() throws Exception {
        // Setup collection path and manifest
        Path tempDir = Files.createTempDirectory("collection");
        Path collectionPath = tempDir.resolve("testCollection");
        Files.createDirectories(collectionPath);

        String manifestContent = "{ \"urisToDelete\": [\"/delete-me\"], \"filesToCopy\": [ { \"source\": \"/source/file.txt\", \"target\": \"/target/file.txt\" } ] }";
        Files.write(collectionPath.resolve("manifest.json"), manifestContent.getBytes());

        // Create a dir to delete
        Path rootFolder = tempDir.resolve("publishing");
        Path deleteDir = rootFolder.resolve("delete-me");
        Files.createDirectories(deleteDir);
        assertTrue("Pre-check: delete dir exists", Files.exists(deleteDir));

        // Create mocked collection
        when(collection.getPath()).thenReturn(collectionPath);

        // Mock ContentReader
        ContentReader mockReader = mock(ContentReader.class);
        when(mockReader.getRootFolder()).thenReturn(rootFolder);

        // Mock Resource for file copy
        Resource mockResource = mock(Resource.class);
        when(mockReader.getResource("/source/file.txt")).thenReturn(mockResource);
        InputStream mockStream = new ByteArrayInputStream("copied content".getBytes());
        when(mockResource.getData()).thenReturn(mockStream);

        // Mock ContentWriter
        ContentWriter mockWriter = mock(ContentWriter.class);

        // Call method
        Method method = PostPublisher.class.getDeclaredMethod("processManifestForMaster", Collection.class, ContentReader.class, ContentWriter.class);
        method.setAccessible(true);
        method.invoke(null, collection, mockReader, mockWriter);

        // Assert deletion
        assertFalse("Expected directory to be deleted", Files.exists(deleteDir));

        // Verify file copy
        verify(mockReader).getResource("/source/file.txt");
        verify(mockResource).getData();
        verify(mockWriter).write(any(InputStream.class), eq("/target/file.txt"));
    }

    private static void setKafkaServiceSupplier(KafkaService kafkaService) throws Exception {
        Field field = PostPublisher.class.getDeclaredField("KAFKA_SERVICE_SUPPLIER");
        field.setAccessible(true);

        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        // Use correct imported ServiceSupplier here
        ServiceSupplier<KafkaService> supplier = () -> kafkaService;
        field.set(null, supplier);
    }

    private static void invokeSendMessage(Collection collection, List<String> uris, String dataType) {
        try {
            Method methodField = PostPublisher.class.getDeclaredMethod("sendMessage", Collection.class, List.class, String.class);
            methodField.setAccessible(true);
            methodField.invoke(null, collection, uris, dataType);
        } catch (Exception e) {
            throw new RuntimeException("Could not invoke sendMessage", e);
        }
    }

    private void invokeCopyFilesToMaster(Zebedee zebedee, Collection collection, CollectionReader collectionReader) {
        try {
            Method method = PostPublisher.class.getDeclaredMethod(
                    "copyFilesToMaster", Zebedee.class, Collection.class, CollectionReader.class);
            method.setAccessible(true);
            method.invoke(null, zebedee, collection, collectionReader);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke copyFilesToMaster", e);
        }
    }

}
