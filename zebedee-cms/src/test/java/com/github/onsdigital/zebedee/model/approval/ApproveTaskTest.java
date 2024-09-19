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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        String result = task.extractDatasetId(fileName);
        String expected = "mret";
        assertThat(result, equalTo(expected));
    }

    @Test
    public void testExtractDatasetVersion() {
        String fileName = "economy/grossdomesticproductgdp/datasets/mycollectionq10/jun2024/mret.csv";
        String result = task.extractDatasetVersion(fileName);
        String expected = "jun2024";
        assertThat(result, equalTo(expected));
    }

    @Test
    public void testExtractDatasetVersion_Simple() {
        String fileName = "jun2024/mret.csv";
        String result = task.extractDatasetVersion(fileName);
        String expected = "jun2024";
        assertThat(result, equalTo(expected));
    }

    @Test
    public void testExtractFileName() {
        String fileName = "economy/grossdomesticproductgdp/datasets/mycollectionq10/jun2024/mret.csv";
        String result = task.extractFileName(fileName);
        String expected = "mret.csv";
        assertThat(result, equalTo(expected));
    }

    @Test
    public void testExtractFileName_Simple() {
        String fileName = "mret.csv";
        String result = task.extractFileName(fileName);
        String expected = "mret.csv";
        assertThat(result, equalTo(expected));
    }

    @Test
    public void testExtractFileName_AnotherSimple() {
        String fileName = "a/b/mret.csv";
        String result = task.extractFileName(fileName);
        String expected = "mret.csv";
        assertThat(result, equalTo(expected));
    }

    @Test
    public void testExtractFileName_YetAnotherSimple() {
        String fileName = "/a/mret.csv";
        String result = task.extractFileName(fileName);
        String expected = "mret.csv";
        assertThat(result, equalTo(expected));
    }

    @Test
    public void testfilePathGenerator(){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date publishDate;
        String today;
        
        try {
            publishDate = sdf.parse("2024-07-18");
            today = sdf.format(new Date());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        
        assertEquals(task.filePathGenerator("mm22", publishDate, "v123", ""), "ts-datasets/mm22/v123");
        assertEquals(task.filePathGenerator("a01jul2025", null, "v123", ""), "ts-datasets/other/2024-09-19/v123");
        assertEquals(task.filePathGenerator("a01jul2025", null, "v123", "v124"), "ts-datasets/other/2024-09-19/v125");
        assertEquals(task.filePathGenerator("x09jul2025", publishDate, "v123", ""), "ts-datasets/other/2024-07-18/v123");
        assertEquals(task.filePathGenerator("dataset1", publishDate, "v123", ""), "ts-datasets/other/2024-07-18/v123");
        assertEquals(task.filePathGenerator("rtisa", publishDate, "v123", ""), "ts-datasets/other/2024-07-18/v123");
        assertEquals(task.filePathGenerator("cla01", publishDate, "v123", ""), "ts-datasets/other/2024-07-18/v123");

        assertEquals(task.filePathGenerator("mm22", publishDate, "v123", "v321"), "ts-datasets/mm22/v322");
        assertEquals(task.filePathGenerator("drsi", publishDate, "v456", "v654"), "ts-datasets/drsi/v655");
        assertEquals(task.filePathGenerator("pn2", publishDate, "current", ""), "ts-datasets/pn2/current");
    }

    @Test
    public void testfilePathGenerator_check_default_value_dataset1() {
        String actual = Configuration.getDataset1ExpectedPath();
        String expected = "economy/inflationandpriceindices/datasets/growthratesofoutputandinputproducerpriceinflation";

        assertEquals(expected, actual);
    }

    @Test
    public void testfilePathGenerator_check_configured_value_dataset1() {
        System.setProperty("EXPECTED_DATASET1_PATH", "economy/inflationandpriceindices/datasets/");
        String actual = Configuration.getDataset1ExpectedPath();
        String expected = "economy/inflationandpriceindices/datasets/";

        assertEquals(expected, actual);
    }

    @Test
    public void testIncrementDatasetVersionByOne_emptyInput() {
        String datasetVersion = "";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> task.incrementDatasetVersionByOne(datasetVersion));
        assertThat(ex.getMessage(), equalTo("input string can't be empty"));
    }

    @Test
    public void testIncrementDatasetVersionByOne_nullInput() {
        String datasetVersion = null;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> task.incrementDatasetVersionByOne(datasetVersion));
        assertThat(ex.getMessage(), equalTo("input string can't be empty"));
    }

    @Test
    public void testIncrementDatasetVersionByOne_badInput() {
        String datasetVersion = "v123abc";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> task.incrementDatasetVersionByOne(datasetVersion));
        assertThat(ex.getMessage(), equalTo("input string is not in the correct format"));
    }

    @Test
    public void testIncrementDatasetVersionByOne_HappyPath() {
        String datasetVersion = "v123";
        String expected = "v124";

        String actual = task.incrementDatasetVersionByOne(datasetVersion);
        assertEquals(expected, actual);
    }

    @Test
    public void testfindCorrectDatasetVersion_nullInput() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> task.findCorrectDatasetVersion(null));
        assertThat(ex.getMessage(), equalTo("input array can't be null"));
    }

    @Test
    public void testfindCorrectDatasetVersion_correctInput() {
        List<String> listOfUris = new ArrayList<>();
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/augusttoseptember2024/diop.csv");
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/augusttoseptember2024/diop.xlsx");
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/augusttoseptember2024/previous/v1/data.json");
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/augusttoseptember2024/previous/v1/diop.csv");

        String expected = "v1";
        String actual = task.findCorrectDatasetVersion(listOfUris);
        assertEquals(expected, actual);
    }

    @Test
    public void testfindCorrectDatasetVersion_anotherCorrectInput_previousVersionExist() {
        List<String> listOfUris = new ArrayList<>();
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/current/diop.csv");
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/current/diop.xlsx");
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/current/previous/v123/data.json");
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/current/previous/v123/diop.csv");

        String expected = "v123";
        String actual = task.findCorrectDatasetVersion(listOfUris);
        assertEquals(expected, actual);
    }

    @Test
    public void testfindCorrectDatasetVersion_anotherCorrectInput_previousVersionDoesNotExist() {
        List<String> listOfUris = new ArrayList<>();
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/current/diop.csv");
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/current/diop.xlsx");
        listOfUris.add("/economy/grossdomesticproductgdp/datasets/mycollectionpagedltest1/september2024/diop.xlsx");

        String expected = "";
        String actual = task.findCorrectDatasetVersion(listOfUris);
        assertEquals(expected, actual);
    }

    @Test
    public void testfindCorrectDatasetVersion_emptyList() {
        List<String> listOfUris = new ArrayList<>();

        String expected = "";
        String actual = task.findCorrectDatasetVersion(listOfUris);
        assertEquals(expected, actual);
    }

}
