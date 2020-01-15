package com.github.onsdigital.zebedee.model.approval;

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
import com.github.onsdigital.zebedee.model.approval.tasks.ReleasePopulator;
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
import com.github.onsdigital.zebedee.util.SlackNotification;
import com.github.onsdigital.zebedee.util.slack.PostMessageField;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
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

    /**
     * @param collection
     * @param session
     * @param collectionReader
     * @param collectionWriter
     * @param publishedReader
     * @param dataIndex
     */
    public ApproveTask(Collection collection, Session session, CollectionReader collectionReader,
                       CollectionWriter collectionWriter, ContentReader publishedReader, DataIndex dataIndex) {
        this(collection, session, collectionReader, collectionWriter, publishedReader, dataIndex,
                getDefaultContentDetailResolver());
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
                ContentDetailResolver contentDetailResolver) {
        this.collection = collection;
        this.session = session;
        this.collectionReader = collectionReader;
        this.collectionWriter = collectionWriter;
        this.publishedReader = publishedReader;
        this.dataIndex = dataIndex;
        this.contentDetailResolver = contentDetailResolver;
    }

    @Override
    public Boolean call() {
        ApprovalEventLog eventLog = null;
        try {
            return doApproval();
        } catch (Exception e) {

            CMSLogEvent errorLog = error().data("collectionId", collection.getId());

            if (session != null && StringUtils.isNotEmpty(session.getEmail())) {
                errorLog.data("approver", session.getEmail());
            }

            if (eventLog != null) {
                errorLog.data("approvalManifest", eventLog);
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
                collectionContent.addAll(collection.getDatasetDetails());
                eventLog.addDatasetDetails();

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

            if (collection == null) {
                return false;
            }
            return true;

        } catch (Exception e) {
            CMSLogEvent errorLog = error().data("collectionId", collection.getDescription().getId());
            if (session != null && StringUtils.isNotEmpty(session.getEmail())) {
                errorLog.data("user", (session.getEmail()));
            }
            if (eventLog != null) {
                errorLog.data("approvalEvents", eventLog != null ? eventLog.logDetails() : null);
            }
            errorLog.logException(e, "approve task: error approving collection reverting collection approval status to ERROR");

            collection.getDescription().setApprovalStatus(ApprovalStatus.ERROR);
            collection.getDescription().addEvent(new Event(APPROVAL_FAILED, session.getEmail(), e));
            try {
                collection.save();
            } catch (Exception e1) {
                error().data("collectionId", collection.getDescription().getId()).data("user", session.getEmail())
                        .logException(e, "approve task: error writing collection to disk after approval exception, you may be " +
                                "required to manually set the collection status to error");
            }

            SlackNotification.collectionAlarm(collection, "Exception approving collection",
                    new PostMessageField("Error", e.getMessage(), false));
            return false;
        }
    }

    public static void generateTimeseries(
            Collection collection,
            ContentReader publishedReader,
            CollectionReader collectionReader,
            CollectionWriter collectionWriter,
            DataIndex dataIndex
    ) throws IOException, ZebedeeException, URISyntaxException {

        // Import any time series update CSV file
        List<TimeseriesUpdateCommand> updateCommands = ImportUpdateCommandCsvs(collection, publishedReader, collectionReader);

        // Generate time series if required.
        new DataPublisher().preprocessCollection(
                publishedReader,
                collectionReader,
                collectionWriter.getReviewed(), true, dataIndex, updateCommands);
    }

    public static List<TimeseriesUpdateCommand> ImportUpdateCommandCsvs(Collection collection, ContentReader publishedReader, CollectionReader collectionReader) throws ZebedeeException, IOException {
        List<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();
        if (collection.description.timeseriesImportFiles != null) {
            info().data("collectionId", collection.getDescription().getId())
                    .log("approve collection: collection contains time series data processing importing CSDB file");

            for (String importFile : collection.getDescription().timeseriesImportFiles) {
                CompoundContentReader compoundContentReader = new CompoundContentReader(publishedReader);
                compoundContentReader.add(collectionReader.getReviewed());

                try (
                        Resource resource = collectionReader.getRoot().getResource(importFile);
                        InputStream csvInput = resource.getData()
                ) {
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

    public static PublishNotification createPublishNotification(
            List<String> uriList,
            Collection collection) {

        // only provide relevent uri's
        //  - remove versioned uris
        //  - add associated uris? /previous /data etc?

        List<ContentDetail> contentToDelete = new ArrayList<>();
        List<PendingDelete> pendingDeletes = collection.getDescription().getPendingDeletes();

        for (PendingDelete pendingDelete : pendingDeletes) {
            ContentTreeNavigator.getInstance().search(pendingDelete.getRoot(), node -> {
                info().data("collectionId", collection.getDescription().getId()).log("adding uri to delete to the publish notification " + node.uri);

                if (!contentToDelete.contains(node.uri)) {
                    ContentDetail contentDetailToDelete = new ContentDetail();
                    contentDetailToDelete.uri = node.uri;
                    contentDetailToDelete.type = node.type;
                    contentToDelete.add(contentDetailToDelete);
                }
            });
        }

        return new PublishNotification(collection, uriList, contentToDelete);
    }

    private void compressZipFiles(Collection collection, CollectionReader collectionReader, CollectionWriter collectionWriter) throws ZebedeeException, IOException {
        TimeSeriesCompressionTask timeSeriesCompressionTask = new TimeSeriesCompressionTask();
        boolean verified = timeSeriesCompressionTask.compressTimeseries(collection, collectionReader, collectionWriter);

        if (!verified) {
            SlackNotification.collectionAlarm(collection,
                    "Failed verification of time series zip files",
                    new PostMessageField("Advice", "Unlock the collection and re-approve to try again", false)
            );
            info().data("collectionId", collection.getDescription().getId())
                    .log("Failed verification of time series zip files");
        }
    }

    public void approveCollection() throws IOException {
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

    public void populateReleasePage(Iterable<ContentDetail> collectionContent) throws IOException {
        // If the collection is associated with a release then populate the release page.
        ReleasePopulator.populateQuietly(collection, collectionReader, collectionWriter, collectionContent);
    }

    public void generatePdfFiles(List<ContentDetail> collectionContent) throws ZebedeeException {
        CollectionPdfGenerator pdfGenerator = new CollectionPdfGenerator(new BabbagePdfService(session, collection));
        pdfGenerator.generatePDFsForCollection(collection, collectionWriter, collectionContent);
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
            throw new IllegalArgumentException("approval task unsuccesful: as session.email required but was null/empty");
        }
        info().data("collectionId", collection.getDescription().getId()).log("approval task: validation sucessful");
    }
}
