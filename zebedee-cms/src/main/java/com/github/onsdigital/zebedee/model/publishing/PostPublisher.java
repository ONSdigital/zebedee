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

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
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

            SlackNotification.publishNotification(publishedCollection, collection.getDescription().publishComplete);

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

            return true;
        } catch (Exception exception) {
            logError(exception, "An error occurred during the publish cleanup")
                    .collectionName(collection).collectionId(collection).log();
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
            logError(e, "An error occurred trying apply the deletes to publishing content ")
                    .collectionName(collection).collectionId(collection).log();
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
            logError(exception, "An error occurred saving publish metrics")
                    .collectionName(publishedCollection.getName()).collectionId(publishedCollection.getId()).log();
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
                    logDebug("Adding uri " + node.uri + " to be archived as part of deleting uri: " + pendingDelete.getRoot().uri);
                    urisToDelete.add(node.uri);
                });

                try {
                    Page page = ContentUtil.deserialiseContent(contentReader.getResource(pendingDelete.getRoot().uri + "/data.json").getData());
                    getDeletedContentService().storeDeletedContent(page, new Date(), urisToDelete, contentReader, collection);
                } catch (ZebedeeException | IOException e) {
                    logError(e, "Failed to stored deleted content for URI: " + pendingDelete.getRoot().uri);
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
                logDebug("Deleting directory: " + target.toString());
                try {
                    FileUtils.deleteDirectory(target.toFile());
                } catch (IOException e) {
                    logError(e, "An error occurred trying to delete directory "
                            + target.toString())
                            .collectionName(collection).collectionId(collection).log();
                }
            }
        } catch (Exception e) {
            logError(e, "An error occurred trying apply the publish manifest deletes to publishing content ")
                    .collectionName(collection).collectionId(collection).log();
        }

    }


    private static void processManifestForMaster(Collection collection, ContentReader contentReader, ContentWriter contentWriter) {

        try {
            Manifest manifest = Manifest.get(collection);

            // Apply any deletes that are defined in the transaction first to ensure we do not delete updated files.
            for (String uri : manifest.urisToDelete) {
                Path target = contentReader.getRootFolder().resolve(StringUtils.removeStart(uri, "/"));
                logDebug("Deleting directory on publishing content: ")
                        .addParameter("path", target.toString())
                        .log();
                try {
                    FileUtils.deleteDirectory(target.toFile());
                } catch (IOException e) {
                    logError(e, "An error occurred trying to delete directory "
                            + target.toString())
                            .collectionName(collection).collectionId(collection).log();
                }
            }

            for (FileCopy fileCopy : manifest.filesToCopy) {
                try (
                        Resource resource = contentReader.getResource(fileCopy.source);
                        InputStream inputStream = resource.getData()
                ) {
                    contentWriter.write(inputStream, fileCopy.target);
                } catch (ZebedeeException | IOException e) {
                    logError(e, "An error occurred trying to copy file from "
                            + fileCopy.source
                            + " to "
                            + fileCopy.target)
                            .collectionName(collection).collectionId(collection).log();
                }
            }
        } catch (Exception e) {
            logError(e, "An error occurred trying apply the publish manifest to publishing content ")
                    .collectionName(collection).collectionId(collection).log();
        }

    }


    private static void indexPublishReport(final Zebedee zebedee, final Path collectionJsonPath, final CollectionReader collectionReader) {
        pool.submit(() -> {
            logInfo("Indexing publish report").log();
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
        logInfo("Unzipping files if required to move to master.").collectionName(collection).collectionId(collection).log();
        for (String uri : collection.reviewed.uris()) {
            Path source = collection.reviewed.get(uri);
            if (source != null) {
                if (source.getFileName().toString().equals("timeseries-to-publish.zip")) {
                    String publishUri = StringUtils.removeStart(StringUtils.removeEnd(uri, "-to-publish.zip"), "/");
                    Path publishPath = zebedee.getPublished().path.resolve(publishUri);
                    logInfo("Unzipping TimeSeries").addParameter("source", source.toString()).addParameter("destination", publishPath.toString()).log();

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

        logInfo("Reindexing search").collectionName(collection).log();
        try {

            long start = System.currentTimeMillis();

            List<String> uris = collection.reviewed.uris("*data.json");
            for (String uri : uris) {
                if (isIndexedUri(uri)) {
                    String contentUri = URIUtils.removeLastSegment(uri);
                    reIndexPublishingSearch(contentUri);
                }
            }

            for (PendingDelete pendingDelete : collection.description.getPendingDeletes()) {

                ContentTreeNavigator.getInstance().search(pendingDelete.getRoot(), node -> {
                    logDebug("Deleting index from publishing search ").addParameter("uri", node.uri).log();
                    pool.submit(() -> {
                        try {
                            Indexer.getInstance().deleteContentIndex(node.type, node.uri);
                        } catch (Exception e) {
                            logError(e, "Exception reloading search index:").log();
                        }
                    });
                });
            }

            logInfo("Redindex search completed").collectionName(collection)
                    .timeTaken((System.currentTimeMillis() - start)).log();

        } catch (Exception exception) {
            logError(exception, "An error occurred during the search reindex").collectionName(collection).log();
            ExceptionUtils.printRootCauseStackTrace(exception);
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
                logError(e, "Exception reloading search index:").log();
            }
        });
    }

    public static void copyFilesToMaster(Zebedee zebedee, Collection collection, CollectionReader collectionReader)
            throws IOException, ZebedeeException {
        logInfo("Moving files from collection into master")
                .collectionId(collection)
                .collectionName(collection)
                .log();
        // Move each item of content:
        for (String uri : collection.reviewed.uris()) {
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
        logInfo("moving collection files to archive for collection")
                .collectionId(collection)
                .collectionName(collection)
                .log();

        String filename = PathUtils.toFilename(collection.getDescription().getName());
        Path collectionJsonSource = zebedee.getCollections().path.resolve(filename + ".json");
        Path collectionFilesSource = collection.reviewed.path;
        Path logPath = zebedee.getPublishedCollections().path;

        if (!Files.exists(logPath)) {
            Files.createDirectory(logPath);
        }

        Date date = new Date();
        String directoryName = format.format(date) + "-" + filename;
        Path collectionFilesDestination = logPath.resolve(directoryName);
        Path collectionJsonDestination = logPath.resolve(directoryName + ".json");

        logInfo("moving collection json")
                .addParameter("from", collectionJsonSource.toString())
                .addParameter("to", collectionJsonDestination.toString())
                .log();
        Files.copy(collectionJsonSource, collectionJsonDestination);

        Path manifestDestination = collectionFilesDestination.resolve(Manifest.filename);
        logInfo("moving manifest json")
                .addParameter("from", Manifest.getManifestPath(collection).toString())
                .addParameter("to", manifestDestination.toString())
                .log();

        FileUtils.copyFile(Manifest.getManifestPath(collection).toFile(), manifestDestination.toFile());

        logInfo("moving collection files")
                .addParameter("from", collectionFilesSource.toString())
                .addParameter("to", collectionFilesDestination.toString())
                .log();
        for (String uri : collection.reviewed.uris()) {
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
