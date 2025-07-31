package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.request.FileCopy;
import com.github.onsdigital.zebedee.json.publishing.request.Manifest;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import com.github.onsdigital.zebedee.service.KafkaService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.service.content.navigation.ContentTreeNavigator;
import com.github.onsdigital.zebedee.util.ContentTree;
import com.github.onsdigital.zebedee.util.SlackNotification;
import com.github.onsdigital.zebedee.util.URIUtils;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.api.Root.zebedee;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Post publish functionality.
 */
public class PostPublisher {

    // The date format including the BST timezone. Dates are stored at UTC and must be formated to take BST into account.
    private static final FastDateFormat FORMAT = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm", TimeZone.getTimeZone("Europe/London"));
    private static final ServiceSupplier<KafkaService> KAFKA_SERVICE_SUPPLIER = () -> ZebedeeCmsService.getInstance().getKafkaService();

    private static final ExecutorService POOL = Executors.newFixedThreadPool(10);

    private static final String TRACE_ID_HEADER = "trace_id";
    private static final String SEARCHINDEX = "ONS";

    private static final String DATASETCONTENTFLAG = "datasets";
    private static final String LEGACYCONTENTFLAG = "legacy";

    /**
     * Do tasks required after a publish takes place.
     *
     * @param zebedee
     * @param collection
     * @param skipVerification
     * @return
     * @throws IOException
     */
    public static boolean postPublish(Zebedee zebedee, Collection collection, boolean skipVerification, CollectionReader collectionReader) throws IOException {

        try {
            PublishedCollection publishedCollection = getPublishedCollection(collection);

            // FIXME using PostPublisher.getPublishedCollection feels a bit hacky
            SlackNotification.publishNotification(publishedCollection,SlackNotification.CollectionStage.POST_PUBLISH, SlackNotification.StageStatus.STARTED);

            ContentReader contentReader = new FileSystemContentReader(zebedee.getPublished().getPath());
            ContentWriter contentWriter = new ContentWriter(zebedee.getPublished().getPath());

            applyDeletesToPublishing(collection, contentReader, contentWriter);
            processManifestForMaster(collection, contentReader, contentWriter);
            copyFilesToMaster(zebedee, collection, collectionReader);

            reindexPublishingSearch(collection);

            // Extract deleted URIs for Kafka
            List<String> deletedUris = extractDeletedUris(collection);

            // Publish content-updated and content-deleted events
            publishKafkaMessagesForCollection(collection, deletedUris);

            Path collectionJsonPath = moveCollectionToArchive(zebedee, collection, collectionReader);

            collection.delete();
            ContentTree.dropCache();
            zebedee.getSchedulerKeyCache().remove(collection.getId());

            SlackNotification.publishNotification(publishedCollection,SlackNotification.CollectionStage.POST_PUBLISH, SlackNotification.StageStatus.COMPLETED);

            return true;
        } catch (Exception exception) {
            error().collectionID(collection).exception(exception).log("An error occurred during the publish cleanup");
            SlackNotification.publishNotification(getPublishedCollection(collection),SlackNotification.CollectionStage.POST_PUBLISH, SlackNotification.StageStatus.FAILED);
        }

        return false;
    }

    private static void publishKafkaMessagesForCollection(Collection collection, List<String> deletedUris) throws IOException {
        if (!CMSFeatureFlags.cmsFeatureFlags().isKafkaEnabled()) {
            return;
        }

        sendContentUpdatedEvents(collection); // for content-updated

        if (!deletedUris.isEmpty()) {
            sendContentDeletedEventsToKafka(collection, deletedUris);// for content-deleted
        }
    }

    private static void applyDeletesToPublishing(Collection collection, ContentReader contentReader, ContentWriter contentWriter) {

        try {
            applyManifestDeletesToMaster(collection, contentReader, contentWriter);
        } catch (Exception e) {
            error().collectionID(collection)
                    .exception(e)
                    .log("An error occurred trying apply the deletes to publishing content");
        }
    }

    public static PublishedCollection getPublishedCollection(Collection collection) throws IOException {
        Path path = Paths.get(collection.getPath().toString() + ".json");
        PublishedCollection publishedCollection;
        try (InputStream input = Files.newInputStream(path)) {
            publishedCollection = ContentUtil.deserialise(input,
                    PublishedCollection.class);
        }
        return publishedCollection;
    }

    private static void applyManifestDeletesToMaster(Collection collection, ContentReader contentReader, ContentWriter contentWriter) {

        try {
            Manifest manifest = Manifest.get(collection);

            // Apply any deletes that are defined in the transaction first to ensure we do not delete updated files.
            for (String uri : manifest.urisToDelete) {
                Path target = contentReader.getRootFolder().resolve(StringUtils.removeStart(uri, "/"));
                info().log("Deleting directory: " + target.toString());
                try {
                    FileUtils.deleteDirectory(target.toFile());
                } catch (IOException e) {
                    error().collectionID(collection)
                            .exception(e)
                            .log("An error occurred trying to delete directory " + target.toString());
                }
            }
        } catch (Exception e) {
            error().collectionID(collection)
                    .exception(e)
                    .log("An error occurred trying apply the publish manifest deletes to publishing content");
        }

    }


    private static void processManifestForMaster(Collection collection, ContentReader contentReader, ContentWriter contentWriter) {

        try {
            Manifest manifest = Manifest.get(collection);

            // Apply any deletes that are defined in the transaction first to ensure we do not delete updated files.
            for (String uri : manifest.urisToDelete) {
                Path target = contentReader.getRootFolder().resolve(StringUtils.removeStart(uri, "/"));
                info().data("path", target.toString()).log("Deleting directory on publishing content: ");
                try {
                    FileUtils.deleteDirectory(target.toFile());
                } catch (IOException e) {
                    error().collectionID(collection)
                            .exception(e)
                            .log("An error occurred trying to delete directory " + target.toString());
                }
            }

            for (FileCopy fileCopy : manifest.filesToCopy) {
                try (
                        Resource resource = contentReader.getResource(fileCopy.source);
                        InputStream inputStream = resource.getData()
                ) {
                    contentWriter.write(inputStream, fileCopy.target);
                } catch (ZebedeeException | IOException e) {
                    error().collectionID(collection)
                            .exception(e)
                            .data("src", fileCopy.source)
                            .data("target", fileCopy.target)
                            .log("An error occurred trying to copy file");
                }
            }
        } catch (Exception e) {
            error().collectionID(collection)
                    .exception(e)
                    .log("An error occurred trying apply the publish manifest to publishing content");
        }

    }

    private static void reindexPublishingSearch(Collection collection) throws IOException {
        info().collectionID(collection).log("Reindexing search");
        try {

            long start = System.currentTimeMillis();

            List<String> uris = collection.getReviewed().uris("*data.json");
            for (String uri : uris) {
                if (isIndexedUri(uri)) {
                    String contentUri = URIUtils.removeLastSegment(uri);
                    reIndexPublishingSearch(contentUri);
                }
            }

            for (PendingDelete pendingDelete : collection.getDescription().getPendingDeletes()) {
                ContentTreeNavigator.getInstance().search(pendingDelete.getRoot(), node -> {
                    info().data("uri", node.uri).log("Deleting index from publishing search ");
                    POOL.submit(() -> {
                        try {
                            Indexer.getInstance().deleteContentIndex(node.getType().getLabel(), node.uri);
                        } catch (Exception e) {
                            error().logException(e, "Exception reloading search index:");
                        }
                    });
                });
            }

            info().collectionID(collection)
                    .data("timeTaken", (System.currentTimeMillis() - start))
                    .log("Redindex search completed");

        } catch (Exception e) {
            error().collectionID(collection)
                    .logException(e, "An error occurred during the search reindex");
        }
    }

    public static List<String> extractDeletedUris(Collection collection) {
        List<String> deletedUris = new ArrayList<>();
        for (PendingDelete pendingDelete : collection.getDescription().getPendingDeletes()) {
            ContentDetail root = pendingDelete.getRoot();
            ContentTreeNavigator.getInstance().search(root, node -> {
                deletedUris.add(node.uri);
            });
        }
        return deletedUris;
    }


    /**
     * Method to determine if a URI is one that should be indexed in search.
     *
     * @param uri
     * @return
     */
    static boolean isIndexedUri(String uri) {
        return !VersionedContentItem.isVersionedUri(uri);
    }

    private static void reIndexPublishingSearch(final String uri) throws IOException {
        POOL.submit(() -> {
            try {
                Indexer.getInstance().reloadContent(uri);
            } catch (Exception e) {
                error().exception(e).log("error reloading search index");
            }
        });
    }

    private static void copyFilesToMaster(Zebedee zebedee, Collection collection, CollectionReader collectionReader)
            throws IOException, ZebedeeException {

        info().collectionID(collection).log("Moving files from collection into master");

        // Move each item of content:
        for (String uri : collection.getReviewed().uris()) {
            if (!VersionedContentItem.isVersionedUri(uri)
                    && !FilenameUtils.getName(uri).equals("timeseries-to-publish.zip")) {
                Path destination = zebedee.getPublished().toPath(uri);
                try (
                        Resource resource = collectionReader.getResource(uri);
                        InputStream dataStream = resource.getData()
                ) {
                    FileUtils.copyInputStreamToFile(dataStream, destination.toFile());
                }
            }
        }
    }

    private static Path moveCollectionToArchive(Zebedee zebedee, Collection collection, CollectionReader collectionReader) throws IOException, ZebedeeException {
        info().collectionID(collection)
                .log("moving collection files to archive for collection");

        String filename = PathUtils.toFilename(collection.getDescription().getName());
        Path collectionJsonSource = zebedee.getCollections().getPath().resolve(filename + ".json");
        Path collectionFilesSource = collection.getReviewed().getPath();
        Path logPath = zebedee.getPublishedCollections().path;

        if (!Files.exists(logPath)) {
            Files.createDirectory(logPath);
        }

        Date date = new Date();
        String directoryName = FORMAT.format(date) + "-" + filename;
        Path collectionFilesDestination = logPath.resolve(directoryName);
        Path collectionJsonDestination = logPath.resolve(directoryName + ".json");

        info().data("from", collectionJsonSource.toString())
                .data("to", collectionJsonDestination.toString())
                .log("moving collection json");

        Files.copy(collectionJsonSource, collectionJsonDestination);

        Path manifestDestination = collectionFilesDestination.resolve(Manifest.filename);
        info().data("from", Manifest.getManifestPath(collection).toString())
                .data("to", manifestDestination.toString())
                .log("moving manifest json");

        FileUtils.copyFile(Manifest.getManifestPath(collection).toFile(), manifestDestination.toFile());

        info().data("from", collectionFilesSource.toString())
                .data("to", collectionFilesDestination.toString())
                .log("moving collection files");

        for (String uri : collection.getReviewed().uris()) {
            try (
                    Resource resource = collectionReader.getResource(uri);
                    InputStream inputStream = resource.getData();
            ) {
                File destination = collectionFilesDestination.resolve(URIUtils.removeLeadingSlash(uri)).toFile();
                FileUtils.copyInputStreamToFile(inputStream, destination);
            }
        }

        return collectionJsonDestination;
    }

    private static void sendContentUpdatedEvents(Collection collection) throws IOException {
        List<ContentDetail> datasetVersionDetails = collection.getDatasetVersionDetails();
        if (datasetVersionDetails != null && !datasetVersionDetails.isEmpty()) {
            List<String> datasetUris = datasetVersionDetails
                    .stream()
                    .map(content -> convertUriForEvent(content.uri))
                    .filter(Publisher::isValidCMDDatasetURI)
                    .collect(Collectors.toList());

            info().data("collectionId", collection.getId())
                    .data("Dataset-uris", datasetUris)
                    .data("publishing", true)
                    .log("converted dataset valid URIs ready for kafka event");
            sendMessage(collection, datasetUris, DATASETCONTENTFLAG);
        }

        List<String> reviewedUris = collection.getReviewed().uris()
                .stream().map(temp -> convertUriForEvent(temp))
                .collect(Collectors.toList());
        info().data("collectionId", collection.getId())
                .data("reviewed-uris-count", reviewedUris.size())
                .data("publishing", true)
                .log("converted reviewed URIs for kafka event");
        sendMessage(collection, reviewedUris, LEGACYCONTENTFLAG);
    }

    /**
     * Prepare a uri for a kafka event
     *
     * @param uri The uri of the published content
     * @return String
     */
    static String convertUriForEvent(String uri) {
        uri = uri.replaceAll("/data.json", "");
        uri.trim();
        return uri;
    }

    // Putting message on kafka
    // This method is taking advantage of MDC to enrich log messages with traceId
    // and passing to kafka events
    private static void sendMessage(Collection collection, List<String> uris, String dataType) {

        String traceId = defaultIfBlank(MDC.get(TRACE_ID_HEADER), UUID.randomUUID().toString());
        info().data("traceId", traceId)
                .log("traceId before sending event");

        try {
            KAFKA_SERVICE_SUPPLIER.getService().produceContentUpdated(collection.getId(), uris, dataType, "",
                    SEARCHINDEX, traceId);
        } catch (Exception e) {
            error()
                    .data("collectionId", collection.getDescription().getId())
                    .data("traceId", traceId)
                    .data("publishing", true)
                    .logException(e, "failed to send content-updated kafka events");

            String channel = Configuration.getDefaultSlackAlarmChannel();
            Notifier notifier = zebedee.getSlackNotifier();
            notifier.sendCollectionAlarm(collection, channel, "Failed to send content-updated kafka events", e);
        }
    }

    private static void sendContentDeletedEventsToKafka(Collection collection, List<String> uris) {
        String traceId = defaultIfBlank(MDC.get(TRACE_ID_HEADER), UUID.randomUUID().toString());

        try {
            KAFKA_SERVICE_SUPPLIER.getService().produceContentDeleted(collection.getId(), uris, SEARCHINDEX, traceId);
            info().data("traceId", traceId)
                    .data("uris", uris)
                    .log("Successfully sent search-content-deleted kafka event");
        } catch (Exception e) {
            error()
                    .data("collectionId", collection.getDescription().getId())
                    .data("traceId", traceId)
                    .data("publishing", true)
                    .data("deletedURIs", uris)
                    .logException(e, "Failed to send search-content-deleted kafka events");

            String channel = Configuration.getDefaultSlackAlarmChannel();
            Notifier notifier = zebedee.getSlackNotifier();
            notifier.sendCollectionAlarm(collection, channel, "Failed to send search-content-deleted kafka events", e);
        }
    }

}
