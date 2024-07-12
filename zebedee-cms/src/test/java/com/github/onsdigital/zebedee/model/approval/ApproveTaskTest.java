package com.github.onsdigital.zebedee.model.approval;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.dp.uploadservice.api.Client;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.model.approval.tasks.CollectionPdfGenerator;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressionTask;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import org.apache.hc.core5.http.NameValuePair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ApproveTaskTest {

    private static final String COLLECTION_ID = "138"; // we are 138
    private static final String EMAIL = "danzig@misfits.com";

    @Mock
    private Collection collection;

    @Mock
    private Content content;

    @Mock
    private CollectionDescription collectionDescription;

    @Mock
    private Session session;

    @Mock
    private CollectionReader collectionReader;

    @Mock
    private CollectionWriter collectionWriter;

    @Mock
    private ContentReader contentReader;

    @Mock
    private ContentWriter contentWriter;

    @Mock
    private DataIndex dataIndex;

    @Mock
    private ContentDetailResolver contentDetailResolver;

    @Mock
    private Notifier slackNotifier;

    @Mock
    private CollectionPdfGenerator pdfGenerator;

    @Mock
    private TimeSeriesCompressionTask compressionTask;

    @Mock
    private Client uploadServiceClient; // Inject the mock Client

    @InjectMocks
    @Spy
    private ApproveTask task;
    
    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    @Captor
    private ArgumentCaptor<List<String>> stringListCaptor;

    @Captor // Declare the ArgumentCaptor
    private ArgumentCaptor<List<NameValuePair>> paramsCaptor; 

    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        this.executorService = Executors.newSingleThreadExecutor();

        when(session.getEmail()).thenReturn(EMAIL);
    }

    @After
    public void tearDown() throws Exception {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        CMSFeatureFlags.reset();
    }

    @Test
    public void createPublishNotificationShouldIncludePendingDeletes() throws Exception {
        // Given a collection that contains pending deletes.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content into
        Collection collection = CollectionTest.createCollection(collectionPath, "createPublishNotificationShouldIncludePendingDeletes");
        String uriToDelete = "some/uri/to/check";
        ContentDetail contentDetail = new ContentDetail("Title", uriToDelete, PageType.DATA_SLICE);
        PendingDelete pendingDelete = new PendingDelete("", contentDetail);
        collection.getDescription().getPendingDeletes().add(pendingDelete);

        // When the publish notification is created as part of the approval process.
        PublishNotification publishNotification = task.createPublishNotification(new ArrayList<>(), collection);

        // Then the publish notification contains the expected directory to delete.
        Assert.assertNotNull(publishNotification);
        Assert.assertTrue(publishNotification.hasUriToDelete(uriToDelete));
    }

    @Test
    public void shouldReturnFalseIfCollecionNull() throws Exception {
        when(collection.getId())
                .thenReturn("1234");
        Future<Boolean> result = executorService.submit(new ApproveTask(collection, null, null, null, null, null, contentDetailResolver, slackNotifier));
        assertFalse(result.get());
    }

    @Test
    public void shouldReturnFalseIfCollectionDescriptionNull() throws Exception {
        Future<Boolean> result = executorService.submit(
                new ApproveTask(collection, null, null, null, null, null, contentDetailResolver, slackNotifier));
        assertFalse(result.get());
    }

    @Test
    public void shouldReturnFalseIfSessionNull() throws Exception {
        when(collection.getDescription()).thenReturn(collectionDescription);
        when(collectionDescription.getId()).thenReturn(COLLECTION_ID);

        Future<Boolean> result = executorService.submit(
                new ApproveTask(collection, null, null, null, null, null, contentDetailResolver, slackNotifier));
        assertFalse(result.get());
    }

    @Test
    public void shouldReturnFalseIfSessionEmailNull() throws Exception {
        when(collection.getDescription()).thenReturn(collectionDescription);
        when(collectionDescription.getId()).thenReturn(COLLECTION_ID);
        when(session.getEmail()).thenReturn(null);

        Future<Boolean> result = executorService.submit(
                new ApproveTask(collection, session, null, null, null, null, contentDetailResolver, slackNotifier));
        assertFalse(result.get());
    }

    @Test
    public void shouldReturnFalseIfContentResolveReturnsIOEx() throws Exception {
        when(collection.getDescription()).thenReturn(collectionDescription);
        when(collectionDescription.getId()).thenReturn(COLLECTION_ID);
        when(contentDetailResolver.resolve(any(), any())).thenThrow(new IOException("CABOOOM!"));

        doNothing().when(collectionDescription).addEvent(eventCaptor.capture());

        Future<Boolean> result = executorService.submit(task);
        assertFalse(result.get());
        assertThat(eventCaptor.getValue().type, equalTo(EventType.APPROVAL_FAILED));
        verify(contentDetailResolver, times(1)).resolve(any(), any());
    }

    @Test
    public void shouldSetCollectionStateToApproved() throws IOException {
        when(collection.getDescription()).thenReturn(collectionDescription);

        doNothing().when(collectionDescription).addEvent(eventCaptor.capture());

        task.approveCollection();

        verify(collectionDescription).setApprovalStatus(ApprovalStatus.COMPLETE);

        List<Event> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents.size(), equalTo(1));
        assertThat(capturedEvents.get(0).getEmail(), equalTo(EMAIL));
        assertThat(capturedEvents.get(0).getType(), equalTo(EventType.APPROVED));

        verify(collection, times(1)).save();
    }

    @Test
    public void shouldSetCollectionStateToErrorIfSaveFails() throws IOException {
        when(collection.getDescription()).thenReturn(collectionDescription);
        when(collection.save()).thenThrow(new RuntimeException());

        doNothing().when(collectionDescription).addEvent(eventCaptor.capture());

        try {
            task.approveCollection();
            fail("Expected IOException");
        } catch (IOException e) {
            InOrder inOrder = Mockito.inOrder(collectionDescription);
            inOrder.verify(collectionDescription).setApprovalStatus(ApprovalStatus.COMPLETE);
            inOrder.verify(collectionDescription).setApprovalStatus(ApprovalStatus.ERROR);

            List<Event> capturedEvents = eventCaptor.getAllValues();
            assertThat(capturedEvents.size(), equalTo(2));

            assertThat(capturedEvents.get(0).getEmail(), equalTo(EMAIL));
            assertThat(capturedEvents.get(0).getType(), equalTo(EventType.APPROVED));
            assertThat(capturedEvents.get(1).getEmail(), equalTo("system"));
            assertThat(capturedEvents.get(1).getType(), equalTo(EventType.APPROVAL_FAILED));
        }
    }

    @Test
    public void shouldApproveSuccessfully() throws Exception {
        when(collection.getDescription()).thenReturn(collectionDescription);
        when(collectionReader.getReviewed()).thenReturn(contentReader);
        when(collectionWriter.getReviewed()).thenReturn(contentWriter);
        when(collection.getReviewed()).thenReturn(content);

        doReturn(pdfGenerator).when(task).getPdfGenerator();
        doReturn(compressionTask).when(task).getCompressionTask();
        doNothing().when(collectionDescription).addEvent(eventCaptor.capture());

        doReturn(new PublishNotification(collection)).when(task).createPublishNotification(stringListCaptor.capture(), eq(collection));

        List<ContentDetail> collectionContent = new ArrayList<>();
        ContentDetail articleDetail = new ContentDetail("Some article", "/the/uri", PageType.ARTICLE);
        collectionContent.add(articleDetail);
        when(contentDetailResolver.resolve(content, contentReader)).thenReturn(collectionContent);

        task.call();

        verify(collection, times(1)).populateReleaseQuietly(collectionReader, collectionWriter, collectionContent);
        verify(pdfGenerator, times(1)).generatePDFsForCollection(collection, contentReader, contentWriter,
                collectionContent);
        verify(compressionTask, times(1)).compressTimeseries(collection, collectionReader, collectionWriter);
        //Verify the collection has been approved
        verify(collectionDescription).setApprovalStatus(ApprovalStatus.COMPLETE);

        List<Event> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents.size(), equalTo(1));
        assertThat(capturedEvents.get(0).getEmail(), equalTo(EMAIL));
        assertThat(capturedEvents.get(0).getType(), equalTo(EventType.APPROVED));

        List<String> notificationUriList = stringListCaptor.getValue();
        assertThat(notificationUriList, hasItem(articleDetail.uri));

        verify(collection, times(1)).save();
    }
    
    @Test
    public void shouldApproveCmdSuccessfully() throws Exception {
        String datasetImportFlag = System.getProperty(CMSFeatureFlags.ENABLE_DATASET_IMPORT);
        System.setProperty(CMSFeatureFlags.ENABLE_DATASET_IMPORT, "true");
        CMSFeatureFlags.reset();
        
        when(collection.getDescription()).thenReturn(collectionDescription);
        when(collectionReader.getReviewed()).thenReturn(contentReader);
        when(collectionWriter.getReviewed()).thenReturn(contentWriter);
        when(collection.getReviewed()).thenReturn(content);

        List<ContentDetail> datasetVersionDetails = new ArrayList<>();
        ContentDetail datasetVersionDetail = new ContentDetail("Dataset version", "/datasets/TS056/editions/2021/versions/4", PageType.API_DATASET_LANDING_PAGE);
        datasetVersionDetails.add(datasetVersionDetail);
        when(collection.getDatasetVersionDetails()).thenReturn(datasetVersionDetails);

        List<ContentDetail> datasetDetails = new ArrayList<>();
        ContentDetail datasetDetail = new ContentDetail("Dataset", "/datasets/TS056/", PageType.API_DATASET_LANDING_PAGE);
        datasetDetails.add(datasetDetail);
        when(collection.getDatasetDetails()).thenReturn(datasetDetails);

        doReturn(pdfGenerator).when(task).getPdfGenerator();
        doReturn(compressionTask).when(task).getCompressionTask();
        doNothing().when(collectionDescription).addEvent(eventCaptor.capture());

        doReturn(new PublishNotification(collection)).when(task).createPublishNotification(stringListCaptor.capture(), eq(collection));

        List<ContentDetail> collectionContent = new ArrayList<>();
        ContentDetail articleDetail = new ContentDetail("Some article", "/the/uri", PageType.ARTICLE);
        collectionContent.add(articleDetail);
        when(contentDetailResolver.resolve(content, contentReader)).thenReturn(collectionContent);

        task.call();

        assertThat(collectionContent, hasItem(datasetVersionDetail));

        verify(collection, times(1)).populateReleaseQuietly(collectionReader, collectionWriter, collectionContent);
        verify(pdfGenerator, times(1)).generatePDFsForCollection(collection, contentReader, contentWriter,
                collectionContent);
        verify(compressionTask, times(1)).compressTimeseries(collection, collectionReader, collectionWriter);
        //Verify the collection has been approved
        verify(collectionDescription).setApprovalStatus(ApprovalStatus.COMPLETE);

        List<Event> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents.size(), equalTo(1));
        assertThat(capturedEvents.get(0).getEmail(), equalTo(EMAIL));
        assertThat(capturedEvents.get(0).getType(), equalTo(EventType.APPROVED));

        List<String> notificationUriList = stringListCaptor.getValue();
        assertThat(notificationUriList, hasItem(articleDetail.uri));
        assertThat(notificationUriList, hasItem(datasetDetail.uri));
        assertThat(notificationUriList, hasItem(datasetVersionDetail.uri));

        verify(collection, times(1)).save();

        if (datasetImportFlag == null) {
            System.clearProperty(CMSFeatureFlags.ENABLE_DATASET_IMPORT);
        } else {
            System.setProperty(CMSFeatureFlags.ENABLE_DATASET_IMPORT, datasetImportFlag);
        }
    }

    // new unit tests

    @Test
    public void testUploadNewEndpoint_HappyPath() throws ZebedeeException, IOException {
        // Given
        Collection collection = Mockito.mock(Collection.class);
        CollectionReader collectionReader = Mockito.mock(CollectionReader.class);
        System.setProperty("ENABLE_UPLOAD_NEW_ENDPOINT", "true");

        when(collection.getDescription()).thenReturn(collectionDescription);
        when(collectionReader.getReviewed()).thenReturn(contentReader);
        when(collectionWriter.getReviewed()).thenReturn(contentWriter);
        when(collection.getReviewed()).thenReturn(content);

        // When
        task.uploadNewEndpoint(collection, collectionReader);

        // Then
        verify(task, times(1)).uploadWhitelistedFiles(collection, collectionReader);
    }

    @Test
    public void testUploadNewEndpoint_UnHappyPath() throws ZebedeeException, IOException {
        // Given
        Collection collection = Mockito.mock(Collection.class);
        CollectionReader collectionReader = Mockito.mock(CollectionReader.class);
        System.setProperty("ENABLE_UPLOAD_NEW_ENDPOINT", "false");

        when(collection.getDescription()).thenReturn(collectionDescription);
        when(collectionReader.getReviewed()).thenReturn(contentReader);
        when(collectionWriter.getReviewed()).thenReturn(contentWriter);
        when(collection.getReviewed()).thenReturn(content);

        // When
        task.uploadNewEndpoint(collection, collectionReader);

        // Then
        verify(task, times(1)).uploadWhitelistedFiles(collection, collectionReader);
    }

    @Test
    public void testCreateUploadParams() {
        String resumableFilename = "test_file.csv";
        String path = "/path/to/file";
        String collectionId = "12345";

        List<NameValuePair> params = ApproveTask.createUploadParams(resumableFilename, path, collectionId);

        assertEquals(7, params.size());

        NameValuePair resumableFilenameParam = params.stream().filter(p -> p.getName().equals("resumableFilename")).findFirst().get();
        assertEquals(resumableFilename, resumableFilenameParam.getValue());

        NameValuePair pathParam = params.stream().filter(p -> p.getName().equals("path")).findFirst().get();
        assertEquals(path, pathParam.getValue());

        NameValuePair collectionIdParam = params.stream().filter(p -> p.getName().equals("collectionId")).findFirst().get();
        assertEquals(collectionId, collectionIdParam.getValue());

        NameValuePair resumableTypeParam = params.stream().filter(p -> p.getName().equals("resumableType")).findFirst().get();
        assertEquals(Configuration.getResumableType(), resumableTypeParam.getValue());
        
        NameValuePair isPublishableParam = params.stream().filter(p -> p.getName().equals("isPublishable")).findFirst().get();
        assertEquals(Configuration.getIsPublishable(), isPublishableParam.getValue());

        NameValuePair licenceParam = params.stream().filter(p -> p.getName().equals("licence")).findFirst().get();
        assertEquals(Configuration.getLicence(), licenceParam.getValue());

        NameValuePair licenceURLParam = params.stream().filter(p -> p.getName().equals("licenceUrl")).findFirst().get();
        assertEquals(Configuration.getLicenceURL(), licenceURLParam.getValue());

    }
    


    @Test
    public void shouldCreateUploadParams() {
        List<NameValuePair> params = ApproveTask.createUploadParams("test.dsri", "path/to/file", "12345");

        assertThat(params.get(0).getName(), equalTo("resumableFilename"));
        assertThat(params.get(0).getValue(), equalTo("test.dsri"));

        assertThat(params.get(1).getName(), equalTo("path"));
        assertThat(params.get(1).getValue(), equalTo("path/to/file"));

        assertThat(params.get(2).getName(), equalTo("collectionId"));
        assertThat(params.get(2).getValue(), equalTo("12345"));

        assertThat(params.get(3).getName(), equalTo("resumableType"));
        assertThat(params.get(3).getValue(), equalTo("text/plain"));

        assertThat(params.get(4).getName(), equalTo("isPublishable"));
        assertThat(params.get(4).getValue(), equalTo("true"));

        assertThat(params.get(5).getName(), equalTo("licence"));
        assertThat(params.get(5).getValue(), equalTo("Open Government Licence v3.0"));

        assertThat(params.get(6).getName(), equalTo("licenceUrl"));
        assertThat(params.get(6).getValue(), equalTo("https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/"));
    }

    @Test
    public void testBaseName() {
        String fileName = "economy/grossdomesticproductgdp/datasets/mycollectionq10/jun2024/mret.csv";
        String baseName = task.baseName(fileName);
        String expected = "mret.csv";
        assertThat(baseName, equalTo(expected));
    }

    @Test
    public void testExtractDatasetId() {
        String fileName = "economy/grossdomesticproductgdp/datasets/mycollectionq10/jun2024/mret.csv";
        String datasetId = task.extractDatasetId(fileName);
        String expected = "mret";
        assertThat(datasetId, equalTo(expected));
    }

    @Test
    public void testExtractDatasetVersion() {
        String fileName = "economy/grossdomesticproductgdp/datasets/mycollectionq10/jun2024/mret.csv";
        String datasetId = task.extractDatasetVersion(fileName);
        String expected = "jun2024";
        assertThat(datasetId, equalTo(expected));
    }

    @Test
    public void testExtractDatasetVersion_Simple() {
        String fileName = "jun2024/mret.csv";
        String datasetId = task.extractDatasetVersion(fileName);
        String expected = "jun2024";
        assertThat(datasetId, equalTo(expected));
    }

    @Test
    public void testExtractFileName() {
        String fileName = "economy/grossdomesticproductgdp/datasets/mycollectionq10/jun2024/mret.csv";
        String datasetId = task.extractFileName(fileName);
        String expected = "mret.csv";
        assertThat(datasetId, equalTo(expected));
    }

    @Test
    public void testExtractFileName_Simple() {
        String fileName = "mret.csv";
        String datasetId = task.extractFileName(fileName);
        String expected = "mret.csv";
        assertThat(datasetId, equalTo(expected));
    }

    @Test
    public void testExtractFileName_AnotherSimple() {
        String fileName = "a/b/mret.csv";
        String datasetId = task.extractFileName(fileName);
        String expected = "mret.csv";
        assertThat(datasetId, equalTo(expected));
    }

    @Test
    public void testExtractFileName_YetAnotherSimple() {
        String fileName = "/a/mret.csv";
        String datasetId = task.extractFileName(fileName);
        String expected = "mret.csv";
        assertThat(datasetId, equalTo(expected));
    }

}