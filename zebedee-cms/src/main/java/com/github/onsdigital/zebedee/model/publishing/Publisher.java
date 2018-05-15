package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.json.publishing.request.Manifest;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.Http;
import com.github.onsdigital.zebedee.util.SlackNotification;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;


public class Publisher {

    private static final List<Host> theTrainHosts;
    private static final ExecutorService pool = Executors.newFixedThreadPool(20);

    // endpoints
    private static final String BEGIN_ENDPOINT = "begin";
    private static final String SEND_MANIFEST_ENDPOINT = "CommitManifest";
    private static final String PUBLISH_ENDPOINT = "publish";
    private static final String COMMIT_ENDPOINT = "commit";
    private static final String ROLLBACK_ENDPOINT = "rollback";

    // parameters
    private static final String ENCRYPTION_PASSWORD_PARAM = "encryptionPassword";
    private static final String TRANSACTION_ID_PARAM = "transactionId";
    private static final String URI_PARAM = "uri";
    private static final String ZIP_PARAM = "zip";

    static {
        theTrainHosts = Configuration.getTheTrainHosts();
        Runtime.getRuntime().addShutdownHook(new ShutDownPublisherThread(pool));
    }

    public static boolean Publish(Collection collection, String email, CollectionReader collectionReader) throws IOException {
        boolean publishComplete = false;

        // First get the in-memory (within-JVM) lock.
        // This will block attempts to write to the collection during the publishing process
        logInfo("PUBLISH: attempting to lock collection for publish").collectionId(collection).log();

        Lock writeLock = collection.getWriteLock();
        writeLock.lock();

        long publishStart = System.currentTimeMillis();

        try {
            // First check the state of the collection
            if (collection.getDescription().publishComplete) {
                logInfo("PUBLISH: collection has already been published. Halting publish")
                        .collectionId(collection)
                        .log();
                return publishComplete;
            }

            // Now attempt to get a file (inter-JVM) lock
            // This prevents Staging and Live attempting to
            // publish the same collection at the same time.
            // We specify WRITE so we can get a lock and
            // CREATE to ensure the file is created if it
            // doesn't exist.
            try (FileChannel channel = FileChannel.open(collection.path.resolve(".lock"), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                 FileLock lock = channel.tryLock()) {
                if (lock != null) {
                    logInfo("PUBLISH: collection lock acquired").collectionId(collection).log();

                    collection.path.resolve("publish.lock");

                    if (collection.getDescription().approvalStatus != ApprovalStatus.COMPLETE) {
                        logInfo("PUBLISH: collection cannot be published as it has not been approved")
                                .collectionId(collection)
                                .log();
                        return false;
                    }

                    publishComplete = publishFilesToWebsite(collection, email, collectionReader);

                    logInfo("PUBLISH: collection publish process completed")
                            .collectionId(collection)
                            .timeTaken((System.currentTimeMillis() - publishStart))
                            .log();

                } else {
                    logInfo("PUBLISH: collection is already locked for publishing. Halting publish attempt")
                            .collectionId(collection)
                            .log();
                }

            } finally {
                // Save any updates to the collection
                logInfo("PUBLISH: saving changes to collection")
                        .addParameter("publishComplete", publishComplete)
                        .collectionId(collection)
                        .log();
                collection.save();
            }

        } finally {
            writeLock.unlock();
            logInfo("PUBLISH: collection lock released")
                    .collectionId(collection)
                    .log();
        }
        return publishComplete;
    }

    /**
     * Submit all files in the collection to the train destination - the website.
     *
     * @param collection The collection to be published.
     * @param email      An identifier for the publishing user.
     * @return If publishing succeeded, true.
     * @throws IOException If a general error occurs.
     */
    public static boolean publishFilesToWebsite(Collection collection, String email, CollectionReader collectionReader) throws IOException {
        boolean publishComplete = false;
        String encryptionPassword = Random.password(100);

        try {
            collection.getDescription().publishStartDate = new Date();
            createPublishingTransactions(collection, encryptionPassword);
            sendManifest(collection, encryptionPassword);
            publishFilteredCollectionFiles(collection, collectionReader, encryptionPassword);

            publishComplete = commitPublish(collection, email, encryptionPassword);
            collection.getDescription().publishEndDate = new Date();

        } catch (Exception e) {
            SlackNotification.alarm(String.format("Exception publishing collection: %s: %s",
                    collection.getDescription().getName(), e.getMessage()));

            // If an error was caught, attempt to roll back the transaction:
            Map<String, String> transactionIds = collection.getDescription().publishTransactionIds;
            if (transactionIds != null && transactionIds.size() > 0) {

                logError(e, "PUBLISH: error while attempting to publish, transaction IDs found for collection " +
                        "attempting to rollback")
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();

                rollbackPublish(collection, encryptionPassword);
            } else {
                logError(e, "PUBLISH: error while attempting to publish, no transaction IDs found for collection " +
                        "no rollback will be attempted")
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();
            }

        } finally {
            // Save any updates to the collection
            try {
                logInfo("PUBLISH: persisting collection changes to disk")
                        .collectionId(collection)
                        .addParameter("publishComplete", publishComplete)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();
                collection.save();
            } catch (Exception e) {
                logError(e, "PUBLISH: error while attempting to persist collection changes to disk")
                        .collectionId(collection)
                        .addParameter("publishComplete", publishComplete)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();
            }
        }

        return publishComplete;
    }

    public static Map<String, String> createPublishingTransactions(Collection collection, String encryptionPassword)
            throws IOException {
        long start = System.currentTimeMillis();
        Map<String, String> hostToTransactionIDMap = new ConcurrentHashMap<>();
        List<Future<IOException>> results = new ArrayList<>();

        for (Host host : theTrainHosts) {
            results.add(pool.submit(() -> {
                IOException result = null;
                try (Http http = new Http()) {
                    logInfo("PUBLISH: creating publishing transaction for collection")
                            .trainHost(host)
                            .collectionId(collection)
                            .log();

                    Endpoint begin = new Endpoint(host, BEGIN_ENDPOINT)
                            .setParameter(ENCRYPTION_PASSWORD_PARAM, encryptionPassword);

                    Response<Result> response = http.post(begin, Result.class);
                    checkResponse(response, null, begin, collection.getDescription().getId());
                    hostToTransactionIDMap.put(host.toString(), response.body.transaction.id);
                } catch (IOException e) {
                    logError(e, "PUBLISH: error while attempting to create new transactions for collection")
                            .trainHost(host)
                            .collectionId(collection)
                            .log();

                    if (collection.getDescription().publishTransactionIds != null
                            && !collection.getDescription().publishTransactionIds.isEmpty()) {
                        logWarn("PUBLISH: clearing existing transactionIDs from collection")
                                .collectionId(collection)
                                .hostToTransactionID(collection.getDescription().publishTransactionIds)
                                .log();

                        collection.getDescription().publishTransactionIds.clear();
                    }
                    result = e;
                }
                return result;
            }));
        }

        checkFutureResults(results, "error creating publishing transaction");
        collection.getDescription().publishTransactionIds = hostToTransactionIDMap;
        collection.save();

        logInfo("PRE-PUBLISH: successfully created publishing transactions for collection")
                .collectionId(collection)
                .hostToTransactionID(hostToTransactionIDMap)
                .timeTaken(System.currentTimeMillis() - start)
                .log();

        return hostToTransactionIDMap;
    }

    /**
     * Publish collection files with required filters applied.
     *
     * @param collection
     * @param collectionReader
     * @param encryptionPassword
     * @throws IOException
     */
    public static void publishFilteredCollectionFiles(Collection collection, CollectionReader collectionReader, String encryptionPassword) throws IOException {
        // We do not want to send versioned files. They have already been taken care of via the manifest.
        // Pass the function to filter files into the publish method.
        Function<String, Boolean> versionedUriFilter = uri -> VersionedContentItem.isVersionedUri(uri);
        Function<String, Boolean> timeseriesUriFilter = uri -> uri.contains("/timeseries/");

        Function<String, Boolean>[] filters = new Function[]{versionedUriFilter, timeseriesUriFilter};

        List<Future<IOException>> results = new ArrayList<>();
        long start = System.currentTimeMillis();

        // Publish each item of content:
        for (String uri : collection.reviewed.uris()) {
            if (!shouldBeFiltered(filters, uri)) {
                //publishFile(collection, encryptionPassword, results, uri, collectionReader);

                Path source = collection.reviewed.get(uri);
                if (source != null) {
                    boolean zipped = false;
                    String publishUri = uri;

                    // if we have a recognised compressed file - set the zip header and set the correct uri so that the files
                    // are unzipped to the correct place.
                    if (source.getFileName().toString().equals("timeseries-to-publish.zip")) {
                        zipped = true;
                        publishUri = StringUtils.removeEnd(uri, "-to-publish.zip");
                    }

                    for (Map.Entry<String, String> entry : collection.getDescription().publishTransactionIds.entrySet()) {
                        Host theTrainHost = new Host(entry.getKey());
                        String transactionId = entry.getValue();

                        results.add(publishFile(collection.getDescription().getId(), theTrainHost,
                                transactionId, encryptionPassword, uri, publishUri, zipped, source, collectionReader));
                    }
                }
            }
        }

        checkFutureResults(results, "error while attempting to publish file");

        logInfo("PUBLISH: successfully sent all publish file requests to the train")
                .collectionId(collection)
                .hostToTransactionID(collection.getDescription().publishTransactionIds)
                .timeTaken((System.currentTimeMillis() - start))
                .log();
    }

    private static Future<IOException> publishFile(
            final String collectionID,
            final Host host,
            final String transactionId,
            final String encryptionPassword,
            final String uri,
            final String publishUri,
            final boolean zipped,
            final Path source,
            final CollectionReader reader
    ) {
        return pool.submit(() -> {
            IOException result = null;
            try (Http http = new Http()) {
                Endpoint publish = new Endpoint(host, PUBLISH_ENDPOINT)
                        .setParameter(TRANSACTION_ID_PARAM, transactionId)
                        .setParameter(ENCRYPTION_PASSWORD_PARAM, encryptionPassword)
                        .setParameter(ZIP_PARAM, Boolean.toString(zipped))
                        .setParameter(URI_PARAM, publishUri);
                try (
                        Resource resource = reader.getResource(uri);
                        InputStream dataStream = resource.getData()
                ) {
                    logInfo("PUBLISH: sending publish collection file request to train host")
                            .collectionId(collectionID)
                            .transactionID(transactionId)
                            .trainHost(host)
                            .addParameter(URI_PARAM, uri)
                            .addParameter("isZip", zipped)
                            .log();

                    Response<Result> response = http.post(publish, dataStream, source.getFileName().toString(), Result.class);
                    checkResponse(response, transactionId, publish, collectionID);
                }
            } catch (IOException e) {
                logError(e, "PUBLISH: error while sending publish file request to train host")
                        .collectionId(collectionID)
                        .transactionID(transactionId)
                        .trainHost(host)
                        .addParameter(URI_PARAM, uri)
                        .addParameter("isZip", zipped)
                        .log();
                result = e;
            }
            return result;
        });
    }

    public static void sendManifest(Collection collection, String encryptionPassword) throws IOException {
        Manifest manifest = Manifest.get(collection);
        List<Future<IOException>> futures = new ArrayList<>();
        long start = System.currentTimeMillis();

        for (Map.Entry<String, String> entry : collection.getDescription().publishTransactionIds.entrySet()) {
            Host theTrainHost = new Host(entry.getKey());
            String transactionId = entry.getValue();

            futures.add(pool.submit(() -> {
                IOException result = null;
                try (Http http = new Http()) {
                    Endpoint publish = new Endpoint(theTrainHost, SEND_MANIFEST_ENDPOINT)
                            .setParameter(TRANSACTION_ID_PARAM, transactionId)
                            .setParameter(ENCRYPTION_PASSWORD_PARAM, encryptionPassword);

                    logInfo("PUBLISH: sending publish manifest to train host")
                            .trainHost(theTrainHost)
                            .collectionId(collection)
                            .log();

                    Response<Result> response = http.postJson(publish, manifest, Result.class);
                    checkResponse(response, transactionId, publish, collection.getDescription().getId());

                } catch (IOException e) {
                    logError(e, "PUBLISH: unexpected error while attempting to send publish manifest to train host")
                            .collectionId(collection)
                            .trainHost(theTrainHost)
                            .transactionID(transactionId)
                            .log();
                    result = e;
                }
                return result;
            }));
        }

        checkFutureResults(futures, "error sending publish manifest");

        logInfo("PUBLISH: successfully sent publish manifest for collection to train hosts")
                .collectionId(collection)
                .hostToTransactionID(collection.getDescription().publishTransactionIds)
                .timeTaken(System.currentTimeMillis() - start)
                .log();
    }

    public static boolean commitPublish(Collection collection, String email, String encryptionPassword) throws IOException {
        long start = System.currentTimeMillis();

        // If all has gone well so far, commit the publishing transaction:
        boolean isSuccess = true;
        for (Result result : commitPublish(collection.getDescription().publishTransactionIds, encryptionPassword)) {
            isSuccess &= !result.error;
            collection.getDescription().AddPublishResult(result);
        }

        if (isSuccess) {
            Date publishedDate = new Date();
            collection.getDescription().addEvent(new Event(publishedDate, EventType.PUBLISHED, email));
            collection.getDescription().publishComplete = true;

            logInfo("PUBLISH: commit publish completed successfully")
                    .collectionId(collection)
                    .timeTaken((System.currentTimeMillis() - start))
                    .log();

            return true;
        } else {
            logWarn("PUBLISH: commit publish was unsuccessfully")
                    .collectionId(collection)
                    .timeTaken((System.currentTimeMillis() - start))
                    .log();
            return false;
        }
    }

    /**
     * If any of the given filters return true, the uri should be filtered
     *
     * @param filters
     * @param uri
     * @return
     */
    private static boolean shouldBeFiltered(Function<String, Boolean>[] filters, String uri) {
        for (Function<String, Boolean> filter : filters) {
            if (filter.apply(uri))
                return true;
        }
        return false;
    }

    /**
     * Commits a publishing transaction.
     *
     * @param transactionIds     The {@link Host}s and transactions to publish to.
     * @param encryptionPassword The password used to encrypt files during publishing.
     * @return The {@link Result} returned by The Train
     * @throws IOException If any errors are encountered in making the request or reported in the {@link Result}.
     */
    static List<Result> commitPublish(Map<String, String> transactionIds, String encryptionPassword) throws IOException {
        List<Result> results = new ArrayList<>();

        List<Future<IOException>> futures = new ArrayList<>();

        for (Map.Entry<String, String> entry : transactionIds.entrySet()) {
            Host host = new Host(entry.getKey());
            String transactionId = entry.getValue();

            futures.add(pool.submit(() -> {
                IOException result = null;
                try {
                    logInfo("PUBLISH: sending commit transaction request to train host")
                            .transactionID(transactionId)
                            .trainHost(host)
                            .log();

                    try (Http http = new Http()) {
                        Endpoint endpoint = new Endpoint(host, COMMIT_ENDPOINT)
                                .setParameter(TRANSACTION_ID_PARAM, transactionId)
                                .setParameter(ENCRYPTION_PASSWORD_PARAM, encryptionPassword);

                        Response<Result> response = http.post(endpoint, Result.class);
                        checkResponse(response, transactionId, endpoint, null);
                        results.add(response.body);
                    }
                } catch (IOException e) {
                    logError(e, "PUBLISH: error while sending commit transaction request to trian host")
                            .trainHost(host)
                            .transactionID(transactionId)
                            .log();
                    result = e;
                }
                return result;
            }));
        }

        checkFutureResults(futures, "error in commit publish");

        logInfo("PUBLISH: successfully committed publishing transaction")
                .hostToTransactionID(transactionIds)
                .log();

        return results;
    }

    /**
     * Rolls back a publishing transaction, suppressing any {@link IOException} and printing it out to the console instead.
     *
     * @param transactionIds     The {@link Host}s and transactions we are attempting to publish to.
     * @param encryptionPassword The password used to encrypt files during publishing.
     */
    public static void rollbackPublish(Collection collection, String encryptionPassword) {
        for (Map.Entry<String, String> entry : collection.getDescription().publishTransactionIds.entrySet()) {
            Host host = new Host(entry.getKey());
            String transactionId = entry.getValue();

            try (Http http = new Http()) {
                Endpoint endpoint = new Endpoint(host, ROLLBACK_ENDPOINT)
                        .setParameter(TRANSACTION_ID_PARAM, transactionId)
                        .setParameter(ENCRYPTION_PASSWORD_PARAM, encryptionPassword);

                logWarn("PUBLISH: sending rollback transaction request for collection")
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();

                Response<Result> response = http.post(endpoint, Result.class);
                checkResponse(response, transactionId, endpoint, null);

                logInfo("PUBLISH: publish rollback request was successful")
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();

            } catch (IOException e) {
                logError(e, "PUBLISH: error rolling back publish transaction")
                        .collectionId(collection)
                        .transactionID(transactionId)
                        .log();
            }
        }
    }

    static void checkResponse(Response<Result> response, String TransactionID, Endpoint endpoint, String collectionID) throws
            IOException {
        if (response.statusLine.getStatusCode() != 200) {
            int code = response.statusLine.getStatusCode();
            String reason = response.statusLine.getReasonPhrase();
            String message = response.body != null ? response.body.message : "";

            URI uri = endpoint.url();
            logError("PUBLISH: request was unsuccessful")
                    .transactionID(TransactionID)
                    .collectionId(collectionID)
                    .addParameter("trainHost", uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort())
                    .addParameter("endpoint", endpoint.url().getPath())
                    .addParameter("statusCode", code)
                    .addParameter("reason", reason)
                    .addParameter("message", message)
                    .log();

            throw new IOException("Error in request: " + code + " " + reason + " " + message);

        } else if (response.body.error == true) {
            throw new IOException("Result error: " + response.body.message);
        } else if (response.body.transaction.errors != null && response.body.transaction.errors.size() > 0) {
            throw new IOException("Transaction error: " + response.body.transaction.errors);
        } else if (response.body.transaction.uriInfos != null) {
            List<String> messages = new ArrayList<>();
            for (UriInfo uriInfo : response.body.transaction.uriInfos) {
                if (StringUtils.isNotBlank(uriInfo.error)) {
                    messages.add("URI error for " + uriInfo.uri + " (" + uriInfo.status + "): " + uriInfo.error);
                }
            }
            if (messages.size() > 0) {
                throw new IOException(messages.toString());
            }
        }
    }

    /**
     * Wait for all results to return, checking if an exception has occurred.
     */
    private static void checkFutureResults(List<Future<IOException>> results, String errorContext) throws IOException {
        for (Future<IOException> result : results) {
            try {
                IOException exception = result.get();
                if (exception != null) {
                    throw exception;
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException(errorContext, e);
            }
        }
    }

    static class ShutDownPublisherThread extends Thread {

        private final ExecutorService executorService;

        public ShutDownPublisherThread(ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public void run() {
            this.executorService.shutdown();
        }
    }
}
