package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
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
import com.github.onsdigital.zebedee.service.DeletedContent.DeletedContentService;
import com.github.onsdigital.zebedee.service.DeletedContent.DeletedContentServiceFactory;
import com.github.onsdigital.zebedee.service.content.navigation.ContentTreeNavigator;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ContentTree;
import com.github.onsdigital.zebedee.util.SlackNotification;
import com.github.onsdigital.zebedee.util.URIUtils;
import com.github.onsdigital.zebedee.util.ZipUtils;
import com.github.onsdigital.zebedee.util.mertics.service.MetricsService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_POST_PUBLISHED_CONFIRMATION;
import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory.getCollectionHistoryDao;

/**
 * Post publish functionality.
 */
public class PostPublisher {

    // The date format including the BST timezone. Dates are stored at UTC and must be formated to take BST into account.
    private static FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm", TimeZone.getTimeZone("Europe/London"));
    private static Session zebdeePublisherSession = null;

    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    private static DeletedContentService deletedContentService;

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

            getCollectionHistoryDao().saveCollectionHistoryEvent(collection, getPublisherClassSession(),
                    COLLECTION_POST_PUBLISHED_CONFIRMATION);

            savePublishMetrics(publishedCollection);

            ContentReader contentReader = new FileSystemContentReader(zebedee.getPublished().path);
            ContentWriter contentWriter = new ContentWriter(zebedee.getPublished().path);

            applyDeletesToPublishing(collection, contentReader, contentWriter);
            processManifestForMaster(collection, contentReader, contentWriter);
            copyFilesToMaster(zebedee, collection, collectionReader);

            reindexPublishingSearch(collection);

            Path collectionJsonPath = moveCollectionToArchive(zebedee, collection, collectionReader);

            if (!skipVerification) {
                // add to published collections list
                indexPublishReport(zebedee, collectionJsonPath, collectionReader);
            }

            collection.delete();
            ContentTree.dropCache();

            // FIXME using PostPublisher.getPublishedCollection feels a bit hacky
            SlackNotification.publishNotification(publishedCollection,SlackNotification.CollectionStage.POST_PUBLISH, SlackNotification.StageStatus.COMPLETED);

            return true;
        } catch (Exception exception) {
            error().data("collectionId", collection.getDescription().getId()).logException(exception, "An error occurred during the publish cleanup");
            // FIXME using PostPublisher.getPublishedCollection feels a bit hacky
            SlackNotification.publishNotification(getPublishedCollection(collection),SlackNotification.CollectionStage.POST_PUBLISH, SlackNotification.StageStatus.FAILED);
        }

        return false;
    }


    /**
     * Returns a session object with the email as the class name of {@link Publisher} - required by the history event
     * logging.
     */
    private static Session getPublisherClassSession() {
        if (zebdeePublisherSession == null) {
            zebdeePublisherSession = new Session();
            zebdeePublisherSession.setEmail(Publisher.class.getName());
        }
        return zebdeePublisherSession;
    }

    private static void applyDeletesToPublishing(Collection collection, ContentReader contentReader, ContentWriter contentWriter) {

        try {
            archiveContentToBeDeleted(collection, contentReader);
            applyManifestDeletesToMaster(collection, contentReader, contentWriter);
        } catch (Exception e) {
            error().data("collectionId", collection.getDescription().getId()).logException(e, "An error occurred trying apply the deletes to publishing content ");
        }
    }

    public static void savePublishMetrics(PublishedCollection publishedCollection) {
        try {
            long publishTimeMs = Math.round(publishedCollection.publishEndDate.getTime() - publishedCollection.publishStartDate.getTime());

            Date publishDate = publishedCollection.getPublishDate();

            if (publishDate == null)
                publishDate = publishedCollection.publishStartDate;

            if (publishDate == null)
                publishDate = new Date();

            MetricsService.getInstance().captureCollectionPublishMetrics(
                    publishedCollection.getId(),
                    publishTimeMs,
                    publishedCollection.publishResults.get(0).transaction.uriInfos.size(),
                    publishedCollection.getType().toString(),
                    publishDate);
        } catch (Exception exception) {
            error().data("collectionId", publishedCollection.getId()).logException(exception, "An error occurred saving publish metrics");
        }
    }

    public static PublishedCollection getPublishedCollection(Collection collection) throws IOException {
        Path path = Paths.get(collection.path.toString() + ".json");
        PublishedCollection publishedCollection;
        try (InputStream input = Files.newInputStream(path)) {
            publishedCollection = ContentUtil.deserialise(input,
                    PublishedCollection.class);
        }
        return publishedCollection;
    }

    private static void archiveContentToBeDeleted(Collection collection, ContentReader contentReader) {

        List<PendingDelete> pendingDeletes = collection.getDescription().getPendingDeletes();

        if (pendingDeletes.size() > 0) {

            for (PendingDelete pendingDelete : pendingDeletes) {

                Set<String> urisToDelete = new HashSet<>();
                ContentTreeNavigator.getInstance().search(pendingDelete.getRoot(), node -> {
                    info().log("Adding uri " + node.uri + " to be archived as part of deleting uri: " + pendingDelete.getRoot().uri);
                    urisToDelete.add(node.uri);
                });

                try {
                    Page page = ContentUtil.deserialiseContent(contentReader.getResource(pendingDelete.getRoot().uri + "/data.json").getData());
                    getDeletedContentService().storeDeletedContent(page, new Date(), urisToDelete, contentReader, collection);
                } catch (ZebedeeException | IOException e) {
                    error().logException(e, "Failed to stored deleted content for URI: " + pendingDelete.getRoot().uri);
                }
            }
        }
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
                    error().data("collectionId", collection.getDescription().getId())
                            .logException(e, "An error occurred trying to delete directory " + target.toString());
                }
            }
        } catch (Exception e) {
            error().data("collectionId", collection.getDescription().getId())
                    .logException(e, "An error occurred trying apply the publish manifest deletes to publishing content ");
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
                    error().data("collectionId", collection.getDescription().getId())
                            .logException(e, "An error occurred trying to delete directory " + target.toString());
                }
            }

            for (FileCopy fileCopy : manifest.filesToCopy) {
                try (
                        Resource resource = contentReader.getResource(fileCopy.source);
                        InputStream inputStream = resource.getData()
                ) {
                    contentWriter.write(inputStream, fileCopy.target);
                } catch (ZebedeeException | IOException e) {
                    error().data("collectionId", collection.getDescription().getId())
                            .logException(e, "An error occurred trying to copy file from " +
                            fileCopy.source + " to " + fileCopy.target);
                }
            }
        } catch (Exception e) {
            error().data("collectionId", collection.getDescription().getId()).logException(e, "An error occurred trying apply the publish manifest to publishing content ");
        }

    }


    private static void indexPublishReport(final Zebedee zebedee, final Path collectionJsonPath, final CollectionReader collectionReader) {
        pool.submit(() -> {
            info().log("Indexing publish report");
            PublishedCollection publishedCollection = zebedee.getPublishedCollections().add(collectionJsonPath);
            if (Configuration.isVerificationEnabled()) {
                zebedee.getVerificationAgent().submitForVerification(publishedCollection, collectionJsonPath, collectionReader);
            }
        });
    }

    /**
     * Timeseries are zipped for the publish to the website, and then need to be unzipped before moving into master
     * on the publishing side
     *
     * @param collection
     * @param zebedee
     * @throws IOException
     */
    private static void unzipTimeseries(Collection collection, CollectionReader collectionReader, Zebedee zebedee) throws IOException, ZebedeeException {
        info().data("collectionId", collection.getDescription().getId()).log("Unzipping files if required to move to master.");

        for (String uri : collection.getReviewed().uris()) {
            Path source = collection.getReviewed().get(uri);
            if (source != null) {
                if (source.getFileName().toString().equals("timeseries-to-publish.zip")) {
                    String publishUri = StringUtils.removeStart(StringUtils.removeEnd(uri, "-to-publish.zip"), "/");
                    Path publishPath = zebedee.getPublished().path.resolve(publishUri);
                    info().data("source", source.toString()).data("destination", publishPath.toString())
                            .log("Unzipping TimeSeries");
                    try (
                            Resource resource = collectionReader.getResource(uri);
                            InputStream dataStream = resource.getData()
                    ) {
                        ZipUtils.unzip(dataStream, publishPath.toString());
                    }
                }
            }
        }
    }


    private static void reindexPublishingSearch(Collection collection) throws IOException {

        info().data("collectionId", collection.getDescription().getId()).log("Reindexing search");
        try {

            long start = System.currentTimeMillis();

            List<String> uris = collection.getReviewed().uris("*data.json");
            for (String uri : uris) {
                if (isIndexedUri(uri)) {
                    String contentUri = URIUtils.removeLastSegment(uri);
                    reIndexPublishingSearch(contentUri);
                }
            }

            for (PendingDelete pendingDelete : collection.description.getPendingDeletes()) {

                ContentTreeNavigator.getInstance().search(pendingDelete.getRoot(), node -> {
                    info().data("uri", node.uri).log("Deleting index from publishing search ");
                    pool.submit(() -> {
                        try {
                            Indexer.getInstance().deleteContentIndex(node.type, node.uri);
                        } catch (Exception e) {
                            error().logException(e, "Exception reloading search index:");
                        }
                    });
                });
            }

            info().data("collectionId", collection.getDescription().getId())
                    .data("timeTaken", (System.currentTimeMillis() - start))
                    .log("Redindex search completed");

        } catch (Exception exception) {
            error().data("collectionId", collection.getDescription().getId())
                    .logException(exception, "An error occurred during the search reindex");
        }
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
        pool.submit(() -> {
            try {
                Indexer.getInstance().reloadContent(uri);
            } catch (Exception e) {
                error().logException(e, "Exception reloading search index:");
            }
        });
    }

    public static void copyFilesToMaster(Zebedee zebedee, Collection collection, CollectionReader collectionReader)
            throws IOException, ZebedeeException {

        info().data("collectionId", collection.getDescription().getId()).log("Moving files from collection into master");

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

    public static Path moveCollectionToArchive(Zebedee zebedee, Collection collection, CollectionReader collectionReader) throws IOException, ZebedeeException {
        info().data("collectionId", collection.getDescription().getId())
                .log("moving collection files to archive for collection");

        String filename = PathUtils.toFilename(collection.getDescription().getName());
        Path collectionJsonSource = zebedee.getCollections().path.resolve(filename + ".json");
        Path collectionFilesSource = collection.getReviewed().path;
        Path logPath = zebedee.getPublishedCollections().path;

        if (!Files.exists(logPath)) {
            Files.createDirectory(logPath);
        }

        Date date = new Date();
        String directoryName = format.format(date) + "-" + filename;
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

    public synchronized static DeletedContentService getDeletedContentService() {

        if (deletedContentService == null)
            deletedContentService = DeletedContentServiceFactory.createInstance();

        return deletedContentService;
    }

}
