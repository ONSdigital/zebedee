package com.github.onsdigital.zebedee.model.approval;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.data.DataPublisher;
import com.github.onsdigital.zebedee.data.importing.CsvTimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.logging.CMSLogEvent;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.CollectionPdfGenerator;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressionTask;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.service.BabbagePdfService;
import com.github.onsdigital.zebedee.service.content.navigation.ContentTreeNavigator;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ContentDetailUtil;
import com.github.onsdigital.zebedee.util.DatasetWhitelistChecker;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import com.github.onsdigital.dp.uploadservice.api.Client;
import com.github.onsdigital.dp.uploadservice.api.APIClient;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.json.EventType.APPROVAL_FAILED;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;

/**
 * Callable implementation for the approval process.
 */
public class ApproveTask implements Callable<Boolean> {

    private final Collection collection;
    private final Session session;
    private final CollectionReader collectionReader;
    private final CollectionWriter collectionWriter;
    private final ContentReader publishedReader;
    private final DataIndex dataIndex;
    private final ContentDetailResolver contentDetailResolver;
    private final Notifier notifier;

    /**
     * @param collection
     * @param session
     * @param collectionReader
     * @param collectionWriter
     * @param publishedReader
     * @param dataIndex
     */
    public ApproveTask(Collection collection, Session session, CollectionReader collectionReader,
            CollectionWriter collectionWriter, ContentReader publishedReader, DataIndex dataIndex, Notifier notifier) {
        this(collection, session, collectionReader, collectionWriter, publishedReader, dataIndex,
                getDefaultContentDetailResolver(), notifier);
    }

    /**
     * @param collection
     * @param session
     * @param collectionReader
     * @param collectionWriter
     * @param publishedReader
     * @param dataIndex
     * @param contentDetailResolver
     */
    ApproveTask(Collection collection, Session session, CollectionReader collectionReader,
            CollectionWriter collectionWriter, ContentReader publishedReader, DataIndex dataIndex,
            ContentDetailResolver contentDetailResolver, Notifier notifier) {
        this.collection = collection;
        this.session = session;
        this.collectionReader = collectionReader;
        this.collectionWriter = collectionWriter;
        this.publishedReader = publishedReader;
        this.dataIndex = dataIndex;
        this.contentDetailResolver = contentDetailResolver;
        this.notifier = notifier;
    }

    @Override
    public Boolean call() {
        try {
            return doApproval();
        } catch (Exception e) {

            CMSLogEvent errorLog = error().data("collectionId", collection.getId());

            if (session != null && StringUtils.isNotEmpty(session.getEmail())) {
                errorLog.data("approver", session.getEmail());
            }

            errorLog.logException(e, "approve task: unrecoverable error while attempting to approve collection");
            return false;
        }
    }

    private boolean doApproval() throws Exception {
        ApprovalEventLog eventLog = null;
        try {
            validate();
            eventLog = new ApprovalEventLog(collection.getDescription().getId(), session.getEmail());

            info().data("collectionId", collection.getDescription().getId())
                    .data("user", session.getEmail()).log("approve task: beginning approval process");

            List<ContentDetail> collectionContent = contentDetailResolver.resolve(collection.getReviewed(),
                    collectionReader.getReviewed());
            eventLog.resolvedDetails();

            if (cmsFeatureFlags().isEnableDatasetImport()) {
                collectionContent.addAll(collection.getDatasetVersionDetails());
                eventLog.addDatasetVersionDetails();
            }

            populateReleasePage(collectionContent);
            eventLog.populatedResleasePage();

            generateTimeseries(collection, publishedReader, collectionReader, collectionWriter, dataIndex);
            eventLog.generatedTimeSeries();

            generatePdfFiles(collectionContent);
            eventLog.generatedPDFs();

            List<String> uriList = collectionContent.stream().map(c -> c.uri).collect(Collectors.toList());
            if (cmsFeatureFlags().isEnableDatasetImport()) {
                // Add dataset page to list of uris to be updated
                uriList.addAll(collection.getDatasetDetails().stream().map(c -> c.uri).collect(Collectors.toList()));
            }
            PublishNotification publishNotification = createPublishNotification(uriList, collection);
            eventLog.createdPublishNotificaion();

            compressZipFiles(collection, collectionReader, collectionWriter);
            eventLog.compressedZipFiles();

            approveCollection();
            eventLog.approvalStateSet();

            // Send a notification to the website with the publish date for caching.
            publishNotification.sendNotification(EventType.APPROVED);
            eventLog.sentPublishNotification();

            eventLog.approvalCompleted();
            info().data("user", session.getEmail()).data("collectionId", collection.getDescription().getId())
                    .log("approve task: collection approve task completed successfully");

            // Use only for upload new endpoint
            if (Configuration.isUploadNewEndpointEnabled()) {
                // get files here?
                String fileName = "";
                for (String string : collectionReader.getReviewed().listUris()) {
                    if (string.contains("csv") || string.contains("xlsl")) {
                        fileName = string.substring(1);
                        Resource myFile = collectionReader.getResource(fileName);
                        if (DatasetWhitelistChecker.isWhitelisted(myFile.getName())) {
                            info().log("File is whitelisted");

                            // File upload functionality
                            File file = new File("afile");
                            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                                IOUtils.copy(myFile.getData(), outputStream);
                            } catch (FileNotFoundException e) {
                                System.out.println("CAN'T FIND IT");
                            } catch (IOException e) {
                                System.out.println("SOMETHING ELSE WENT WRONG");
                            }

                            List<NameValuePair> params = createUploadParams(fileName, "text/plain", "testing", "true",
                                    "text/plain", "TestLicence", "google", collection.getDescription().getId());

                            Client uploadServiceClient = new APIClient("http://dp-upload-service:25100/upload-new",
                                    "664bff26407d60d5605f64379e47495c0c533c1565042d70653f31c0c705726f");
                            uploadServiceClient.uploadResumableFile(file, params);

                        } else {
                            info().log("File is not whitelisted");
                        }
                        break;
                    }
                }
            }

            return collection != null;

        } catch (Exception e) {
            String channel = Configuration.getDefaultSlackAlarmChannel();
            notifier.sendCollectionAlarm(collection, channel, "Error approving collection", e);

            CMSLogEvent errorLog = error().data("collectionId", collection.getDescription().getId());
            if (session != null && StringUtils.isNotEmpty(session.getEmail())) {
                errorLog.data("user", (session.getEmail()));
            }
            if (eventLog != null) {
                errorLog.data("approvalEvents", eventLog != null ? eventLog.logDetails() : null);
            }
            errorLog.logException(e,
                    "approve task: error approving collection reverting collection approval status to ERROR");

            collection.getDescription().setApprovalStatus(ApprovalStatus.ERROR);
            collection.getDescription().addEvent(new Event(APPROVAL_FAILED, session.getEmail(), e));
            try {
                collection.save();
            } catch (Exception e1) {
                error().data("collectionId", collection.getDescription().getId()).data("user", session.getEmail())
                        .logException(e,
                                "approve task: error writing collection to disk after approval exception, you may be " +
                                        "required to manually set the collection status to error");
            }
            return false;
        }
    }

    public static void generateTimeseries(
            Collection collection,
            ContentReader publishedReader,
            CollectionReader collectionReader,
            CollectionWriter collectionWriter,
            DataIndex dataIndex) throws IOException, ZebedeeException, URISyntaxException {

        // Import any time series update CSV file
        List<TimeseriesUpdateCommand> updateCommands = importUpdateCommandCsvs(collection, publishedReader,
                collectionReader);

        // Generate time series if required.
        new DataPublisher().preprocessCollection(
                publishedReader,
                collectionReader,
                collectionWriter.getReviewed(), true, dataIndex, updateCommands);
    }

    public static List<TimeseriesUpdateCommand> importUpdateCommandCsvs(Collection collection,
            ContentReader publishedReader,
            CollectionReader collectionReader)
            throws ZebedeeException, IOException {

        List<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();
        if (collection.getDescription().getTimeseriesImportFiles() != null
                && !collection.getDescription().getTimeseriesImportFiles().isEmpty()) {

            info().data("collectionId", collection.getDescription().getId())
                    .log("approve collection: collection contains time series data processing importing CSDB file");

            for (String importFile : collection.getDescription().getTimeseriesImportFiles()) {
                CompoundContentReader compoundContentReader = new CompoundContentReader(publishedReader);
                compoundContentReader.add(collectionReader.getReviewed());

                try (
                        Resource resource = collectionReader.getRoot().getResource(importFile);
                        InputStream csvInput = resource.getData()) {
                    // read the CSV and update the timeseries titles.
                    TimeseriesUpdateImporter importer = new CsvTimeseriesUpdateImporter(csvInput);

                    info().data("filename", importFile).data("collectionId", collection.getDescription().getId())
                            .log("approve collection: importing csv file");

                    updateCommands.addAll(importer.importData());
                }
            }
        }
        return updateCommands;
    }

    protected PublishNotification createPublishNotification(
            List<String> uriList,
            Collection collection) {

        // only provide relevent uri's
        // - remove versioned uris
        // - add associated uris? /previous /data etc?

        List<ContentDetail> contentToDelete = new ArrayList<>();
        List<PendingDelete> pendingDeletes = collection.getDescription().getPendingDeletes();

        for (PendingDelete pendingDelete : pendingDeletes) {
            ContentTreeNavigator.getInstance().search(pendingDelete.getRoot(), node -> {
                info().data("collectionId", collection.getDescription().getId())
                        .log("adding uri to delete to the publish notification " + node.uri);

                if (!contentToDelete.contains(node.uri)) {
                    ContentDetail contentDetailToDelete = new ContentDetail(node.uri, node.getType());
                    contentToDelete.add(contentDetailToDelete);
                }
            });
        }

        return new PublishNotification(collection, uriList, contentToDelete);
    }

    private void compressZipFiles(Collection collection, CollectionReader collectionReader,
            CollectionWriter collectionWriter) throws ZebedeeException, IOException {
        boolean verified = getCompressionTask().compressTimeseries(collection, collectionReader, collectionWriter);

        if (!verified) {
            String channel = Configuration.getDefaultSlackAlarmChannel();
            notifier.sendCollectionAlarm(collection, channel, "Failed verification of time series zip files");
            info().data("collectionId", collection.getDescription().getId())
                    .log("Failed verification of time series zip files");
        }
    }

    protected void approveCollection() throws IOException {
        // set the approved state on the collection
        try {
            collection.getDescription().setApprovalStatus(ApprovalStatus.COMPLETE);
            collection.getDescription().addEvent(new Event(new Date(), EventType.APPROVED, session.getEmail()));
            collection.save();
        } catch (Exception ex) {
            error().exception(ex).collectionID(collection).log("error saving collection during approval");
            collection.getDescription().setApprovalStatus(ApprovalStatus.ERROR);
            collection.getDescription().addEvent(new Event(new Date(), EventType.APPROVAL_FAILED, "system"));
            throw new IOException(ex);
        }
    }

    private void populateReleasePage(Iterable<ContentDetail> collectionContent) throws IOException {
        // If the collection is associated with a release then populate the release
        // page.
        collection.populateReleaseQuietly(collectionReader, collectionWriter, collectionContent);
    }

    private void generatePdfFiles(List<ContentDetail> collectionContent) throws ZebedeeException {
        getPdfGenerator().generatePDFsForCollection(collection, collectionReader.getReviewed(),
                collectionWriter.getReviewed(), collectionContent);
    }

    protected CollectionPdfGenerator getPdfGenerator() {
        return new CollectionPdfGenerator(new BabbagePdfService(session, collection));
    }

    protected TimeSeriesCompressionTask getCompressionTask() {
        return new TimeSeriesCompressionTask();
    }

    private static ContentDetailResolver getDefaultContentDetailResolver() {
        return (content, reader) -> new ArrayList<>(ContentDetailUtil.resolveDetails(content, reader));
    }

    private void validate() {
        if (collection == null) {
            throw new IllegalArgumentException("approval task unsuccesful: collection required but was null");
        }
        if (collection.getDescription() == null) {
            throw new IllegalArgumentException("approval task unsuccesful: collection.description required but was " +
                    "null");
        }
        if (session == null) {
            throw new IllegalArgumentException("approval task unsuccesful: as session required but was null");
        }
        if (StringUtils.isEmpty(session.getEmail())) {
            throw new IllegalArgumentException(
                    "approval task unsuccesful: as session.email required but was null/empty");
        }
        info().data("collectionId", collection.getDescription().getId()).log("approval task: validation sucessful");
    }

    protected static List<NameValuePair> createUploadParams(String resumableFilename,
            String path,
            //String type,
            String collectionId) {

        // Get the following values from the config
        String resumableType = Configuration.getResumableType();
        String isPublishable = Configuration.getIsPublishable();
        String licence = Configuration.getLicence();
        String licenceURL= Configuration.getLicenceURL();

        List<NameValuePair> params = new ArrayList<>(8);
        params.add(new BasicNameValuePair("resumableFilename", resumableFilename));
        params.add(new BasicNameValuePair("path", path));
        params.add(new BasicNameValuePair("collectionId", collectionId));
        params.add(new BasicNameValuePair("resumableType", resumableType));
        params.add(new BasicNameValuePair("isPublishable", isPublishable));
        // params.add(new BasicNameValuePair("type", type));
        params.add(new BasicNameValuePair("licence", licence));
        params.add(new BasicNameValuePair("licenceUrl", licenceURL));
        return params;
    }
}

