package com.github.onsdigital.zebedee.model.approval;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionTest;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.CollectionPdfGenerator;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressionTask;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.DatasetWhitelistChecker;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import com.github.onsdigital.dp.uploadservice.api.Client;

import org.apache.hc.core5.http.NameValuePair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    @Spy
    private ApproveTask task;
    
    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    @Captor
    private ArgumentCaptor<List<String>> stringListCaptor;

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
    public void testUploadNewEndpoint() throws ZebedeeException, IOException {
        // Given
        Collection collection = Mockito.mock(Collection.class);
        CollectionReader collectionReader = Mockito.mock(CollectionReader.class);
        when(Configuration.isUploadNewEndpointEnabled()).thenReturn(true);

        // When
        task.uploadNewEndpoint(collection, collectionReader);

        // Then
        verify(task, times(1)).uploadWhitelistedFiles(collection, collectionReader);
    }

    @Test
    public void testUploadWhitelistedFiles() throws ZebedeeException, IOException {
        // Given
        Collection collection = Mockito.mock(Collection.class);
        CollectionReader collectionReader = Mockito.mock(CollectionReader.class);
        String uri = "some/uri";
        Resource myFile = Mockito.mock(Resource.class);
        when(collectionReader.getReviewed().listUris()).thenReturn(Collections.singletonList(uri));
        when(collectionReader.getResource(uri)).thenReturn(myFile);
        when(DatasetWhitelistChecker.isWhitelisted(myFile.getName())).thenReturn(true);

        // When
        task.uploadWhitelistedFiles(collection, collectionReader);

        // Then
        verify(task, times(1)).uploadFile(myFile, uri, collection.getDescription().getId());
    }

    @Test
    public void testUploadFile() throws ZebedeeException, IOException {
        // Given
        Resource myFile = Mockito.mock(Resource.class);
        String fileName = "filename.drsi"; 
        String collectionId = "123";
        File file = new File("afile");
        when(myFile.getData()).thenReturn(new ByteArrayInputStream("some data".getBytes()));
        Client uploadServiceClient = Mockito.mock(Client.class);
        
        // When
        task.uploadFile(myFile, fileName, collectionId);

        // Then
        verify(uploadServiceClient, times(1)).uploadResumableFile(file, ApproveTask.createUploadParams(fileName, "path", collectionId));
    }
}