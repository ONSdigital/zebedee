package com.github.onsdigital.zebedee.model.publishing;

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
import com.github.onsdigital.zebedee.service.DatasetService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.util.Http;
import com.github.onsdigital.zebedee.util.SlackNotification;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import com.github.onsdigital.zebedee.util.slack.PostMessageField;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;
import static com.github.onsdigital.zebedee.model.publishing.PostPublisher.getPublishedCollection;
import static com.github.onsdigital.zebedee.util.SlackNotification.CollectionStage.PUBLISH;
import static com.github.onsdigital.zebedee.util.SlackNotification.StageStatus.FAILED;
import static com.github.onsdigital.zebedee.util.SlackNotification.StageStatus.STARTED;
import static com.github.onsdigital.zebedee.util.SlackNotification.collectionAlarm;
import static com.github.onsdigital.zebedee.util.SlackNotification.publishNotification;


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
    private static final String TRANSACTION_ID_PARAM = "transactionId";
    private static final String URI_PARAM = "uri";
    private static final String ZIP_PARAM = "zip";

    private static ServiceSupplier<DatasetService> datasetServiceSupplier;

    static {
        theTrainHosts = Configuration.getTheTrainHosts();
        Runtime.getRuntime().addShutdownHook(new ShutDownPublisherThread(pool));

        // lazy loaded approach for getting the datasetService.
        datasetServiceSupplier = () -> ZebedeeCmsService.getInstance().getDatasetService();
    }

    /**
     * Execute the prepublish steps.
     */
    public static void executePrePublish(Collection collection) throws IOException {
        collection.getDescription().publishStartDate = new Date();
        createPublishingTransactions(collection);
        sendManifest(collection);
    }

    /**
     * Execute the publishing steps.
     */
    public static boolean executePublish(Collection collection, CollectionReader collectionReader, String email)
            throws IOException {
        publishFilteredCollectionFiles(collection, collectionReader);

        boolean publishComplete = commitPublish(collection, email);

        // FIXME CMD feature
        if (Configuration.isEnableDatasetImport()) {
            publishComplete &= publishDatasets(collection);
        }

        logInfo("collection publish time")
                .addParameter("milliseconds", collection.getPublishTimeMilliseconds())
                .addParameter("publishComplete", publishComplete)
                .publishingAction()
                .collectionId(collection)
                .collectionName(collection)
                .log();

        return publishComplete;
    }

    /**
     * TODO
     */
    public static boolean publish(Collection collection, String email, CollectionReader collectionReader) throws
            IOException {
        // FIXME using PostPublisher.getPublishedCollection feels a bit hacky
        publishNotification(getPublishedCollection(collection), PUBLISH, STARTED);

        boolean publishComplete = false;

        // First get the in-memory (within-JVM) lock.
        // This will block attempts to write to the collection during the publishAction process
        logInfo("attempting to lock collection for publish")
                .publishingAction()
                .collectionId(collection).log();

        Lock writeLock = collection.getWriteLock();
        writeLock.lock();

        long publishStart = System.currentTimeMillis();

        try {
            // First check the state of the collection
            if (isPublished(collection)) {
                return false;
            }

            // Now attempt to get a file (inter-JVM) lock. This prevents Staging and Live attempting to publish the
            // same collection at the same time. We specify WRITE so we can get a lock and CREATE to ensure the file
            // is created if it doesn't exist.
            Path collectionLock = collection.path.resolve(".lock");
            try (FileChannel channel = FileChannel.open(collectionLock, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                 FileLock lock = channel.tryLock()) {

                if (lock != null) {
                    logInfo("collection lock acquired")
                            .publishingAction()
                            .collectionId(collection)
                            .log();

                    if (!isApproved(collection)) {
                        return false;
                    }

                    publishComplete = publishFilesToWebsite(collection, email, collectionReader);

                    logInfo("collection publish process completed")
                            .publishingAction()
                            .collectionId(collection)
                            .timeTaken((System.currentTimeMillis() - publishStart))
                            .log();

                } else {
                    logInfo("collection is already locked for publishAction, halting publish attempt")
                            .publishingAction()
                            .collectionId(collection)
                            .log();
                }

            } finally {
                saveCollection(collection, publishComplete);
            }

        } finally {
            writeLock.unlock();
            logInfo("collection lock released").publishingAction().collectionId(collection).log();
        }

        SlackNotification.StageStatus status = FAILED;
        if (publishComplete) {
            status = SlackNotification.StageStatus.COMPLETED;
        }
        // FIXME using PostPublisher.getPublishedCollection feels a bit hacky
        publishNotification(getPublishedCollection(collection), PUBLISH, status);

        return publishComplete;
    }

    /**
     * Return true if the collection approval status equals {@link ApprovalStatus#COMPLETE}, return false otherwise.
     */
    private static boolean isApproved(Collection collection) {
        if (collection.getDescription().approvalStatus != ApprovalStatus.COMPLETE) {
            logInfo("collection cannot be published as it has not been approved")
                    .publishingAction()
                    .collectionId(collection)
                    .log();
            return false;
        }
        return true;
    }

    /**
     * Return true if collection is already marked as publish completed, return false otherwise
     */
    private static boolean isPublished(Collection collection) {
        if (collection.getDescription().publishComplete) {
            logInfo("collection has already been published, halting publish")
                    .publishingAction()
                    .collectionId(collection)
                    .log();
            return true;
        }
        return false;
    }

    /**
     * Submit all files in the collection to the train destination - the website.
     *
     * @param collection The collection to be published.
     * @param email      An identifier for the publishAction user.
     * @return If publishAction succeeded, true.
     * @throws IOException If a general error occurs.
     */
    public static boolean publishFilesToWebsite(Collection collection, String email, CollectionReader collectionReader) throws IOException {
        boolean publishComplete = false;

        try {
            executePrePublish(collection);
            publishComplete = executePublish(collection, collectionReader, email);
        } catch (Exception e) {
            PostMessageField msg = new PostMessageField("Error", e.getMessage(), false);
            collectionAlarm(collection, "Exception publishAction collection", msg);

            // If an error was caught, attempt to roll back the transaction:
            Map<String, String> transactionIds = collection.getDescription().publishTransactionIds;
            if (transactionIds != null && transactionIds.size() > 0) {

                logError(e, "error while attempting to publish, transaction IDs found for collection attempting to " +
                        "rollback")
                        .publishingAction()
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();

                rollbackPublish(collection);
            } else {
                logError(e, "error while attempting to publish, no transaction IDs found for collection " +
                        "no rollback will be attempted")
                        .publishingAction()
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();
            }

        } finally {
            // Save any updates to the collection
            saveCollection(collection, publishComplete);
        }
        return publishComplete;
    }

    public static Map<String, String> createPublishingTransactions(Collection collection)
            throws IOException {
        long start = System.currentTimeMillis();
        Map<String, String> hostToTransactionIDMap = new ConcurrentHashMap<>();
        List<Future<IOException>> results = new ArrayList<>();

        for (Host host : theTrainHosts) {
            results.add(pool.submit(() -> {
                IOException result = null;
                try (Http http = new Http()) {
                    logInfo("creating publish transaction for collection")
                            .publishingAction()
                            .trainHost(host)
                            .collectionId(collection)
                            .log();

                    Endpoint begin = new Endpoint(host, BEGIN_ENDPOINT);

                    Response<Result> response = http.post(begin, Result.class);
                    checkResponse(response, null, begin, collection.getDescription().getId());
                    hostToTransactionIDMap.put(host.toString(), response.body.transaction.id);
                } catch (IOException e) {
                    logError(e, "error while attempting to create new transactions for collection")
                            .publishingAction()
                            .trainHost(host)
                            .collectionId(collection)
                            .log();

                    if (collection.getDescription().publishTransactionIds != null
                            && !collection.getDescription().publishTransactionIds.isEmpty()) {
                        logWarn("clearing existing transactionIDs from collection")
                                .publishingAction()
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

        checkFutureResults(results, "error creating publishAction transaction");
        collection.getDescription().publishTransactionIds = hostToTransactionIDMap;
        collection.save();

        logInfo("successfully created publish transactions for collection")
                .publishingAction()
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
     * @throws IOException
     */
    public static void publishFilteredCollectionFiles(Collection collection, CollectionReader collectionReader) throws IOException {
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
                                transactionId, uri, publishUri, zipped, source, collectionReader));
                    }
                }
            }
        }

        checkFutureResults(results, "error while attempting to publish file");

        logInfo("successfully sent all publish file requests to the train")
                .publishingAction()
                .collectionId(collection)
                .hostToTransactionID(collection.getDescription().publishTransactionIds)
                .timeTaken((System.currentTimeMillis() - start))
                .log();
    }

    private static Future<IOException> publishFile(
            final String collectionID,
            final Host host,
            final String transactionId,
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
                        .setParameter(ZIP_PARAM, Boolean.toString(zipped))
                        .setParameter(URI_PARAM, publishUri);
                try (
                        Resource resource = reader.getResource(uri);
                        InputStream dataStream = resource.getData()
                ) {
                    logInfo("sending publish collection file request to train host")
                            .publishingAction()
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
                logError(e, "error while sending publish file request to train host")
                        .publishingAction()
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

    public static void sendManifest(Collection collection) throws IOException {
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
                            .setParameter(TRANSACTION_ID_PARAM, transactionId);

                    logInfo("sending publish manifest to train host")
                            .publishingAction()
                            .trainHost(theTrainHost)
                            .collectionId(collection)
                            .log();

                    Response<Result> response = http.postJson(publish, manifest, Result.class);
                    checkResponse(response, transactionId, publish, collection.getDescription().getId());

                } catch (IOException e) {
                    logError(e, "unexpected error while attempting to send publish manifest to train host")
                            .publishingAction()
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

        logInfo("successfully sent publish manifest for collection to train hosts")
                .publishingAction()
                .collectionId(collection)
                .hostToTransactionID(collection.getDescription().publishTransactionIds)
                .timeTaken(System.currentTimeMillis() - start)
                .log();
    }

    public static boolean commitPublish(Collection collection, String email) throws IOException {
        long start = System.currentTimeMillis();

        // If all has gone well so far, commit the publishAction transaction:
        boolean isSuccess = true;
        for (Result result : commitPublish(collection.getDescription().publishTransactionIds)) {
            isSuccess &= !result.error;
            collection.getDescription().AddPublishResult(result);
        }

        if (isSuccess) {
            Date publishedDate = new Date();
            collection.getDescription().addEvent(new Event(publishedDate, EventType.PUBLISHED, email));
            collection.getDescription().publishComplete = true;

            logInfo("commit publish completed successfully")
                    .publishingAction()
                    .collectionId(collection)
                    .timeTaken((System.currentTimeMillis() - start))
                    .log();

            return true;
        } else {
            logWarn("commit publish was unsuccessfully")
                    .publishingAction()
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
     * Commits a publishAction transaction.
     *
     * @param transactionIds The {@link Host}s and transactions to publish to.
     * @return The {@link Result} returned by The Train
     * @throws IOException If any errors are encountered in making the request or reported in the {@link Result}.
     */
    static List<Result> commitPublish(Map<String, String> transactionIds) throws IOException {
        List<Callable<Result>> commitTasks = transactionIds.entrySet()
                .stream()
                .map(entry -> {
                    Host host = new Host(entry.getKey());
                    String transactionId = entry.getValue();

                    logDebug("creating commit publish transaction tasks")
                            .publishingAction()
                            .transactionID(transactionId)
                            .trainHost(host)
                            .log();

                    Callable<Result> task = () -> {
                        try {
                            logInfo("sending commit transaction request to train host")
                                    .publishingAction()
                                    .transactionID(transactionId)
                                    .trainHost(host)
                                    .log();

                            try (Http http = new Http()) {
                                Endpoint endpoint = new Endpoint(host, COMMIT_ENDPOINT)
                                        .setParameter(TRANSACTION_ID_PARAM, transactionId);

                                Response<Result> response = http.post(endpoint, Result.class);
                                checkResponse(response, transactionId, endpoint, null);
                                return response.body;
                            }
                        } catch (Exception e) {
                            logError(e, "error while sending commit transaction request to trian host")
                                    .publishingAction()
                                    .trainHost(host)
                                    .transactionID(transactionId)
                                    .log();
                            throw new IOException(e);
                        }
                    };
                    return task;
                }).collect(Collectors.toList());

        logDebug("submitting commit publish tasks")
                .publishingAction()
                .addParameter("count", commitTasks.size())
                .log();

        List<Future<Result>> futures = new ArrayList<>();
        try {
            futures = pool.invokeAll(commitTasks);
        } catch (Exception e) {
            logError(e, "error invoking commit tasks")
                    .publishingAction()
                    .log();
            throw new IOException(e);
        }

        logDebug("checking commit publish tasks results")
                .publishingAction()
                .log();

        List<Result> results = new ArrayList<>();
        for (Future<Result> resultFuture : futures) {
            try {
                Result r = resultFuture.get();
                if (r == null) {
                    throw new IOException("commit publish result was null");
                }
                logInfo("adding commit publish result future to list of results")
                        .publishingAction()
                        .transactionID(r.transaction.id)
                        .addParameter("isError", r.error)
                        .log();

                results.add(r);
            } catch (Exception e) {
                logError(e, "commit tranaction future throw unexpected exception")
                        .publishingAction()
                        .log();
                throw new IOException(e);
            }
        }

        logInfo("successfully committed publishAction transaction")
                .publishingAction()
                .hostToTransactionID(transactionIds)
                .log();

        return results;
    }

    /**
     * Rolls back a publishAction transaction, suppressing any {@link IOException} and printing it out to the console instead.
     *
     * @param collection the collection to roll back
     */
    public static void rollbackPublish(Collection collection) {
        for (Map.Entry<String, String> entry : collection.getDescription().publishTransactionIds.entrySet()) {
            Host host = new Host(entry.getKey());
            String transactionId = entry.getValue();

            try (Http http = new Http()) {
                Endpoint endpoint = new Endpoint(host, ROLLBACK_ENDPOINT)
                        .setParameter(TRANSACTION_ID_PARAM, transactionId);

                logWarn("sending rollback transaction request for collection")
                        .publishingAction()
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();

                Response<Result> response = http.post(endpoint, Result.class);
                checkResponse(response, transactionId, endpoint, null);

                logInfo("publish rollback request was successful")
                        .publishingAction()
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();

            } catch (IOException e) {
                logError(e, "error rolling back publish transaction")
                        .publishingAction()
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
            logError("request was unsuccessful")
                    .publishingAction()
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

    private static boolean publishDatasets(Collection collection) throws IOException {
        logDebug("publishing api datasets for collection")
                .collectionName(collection)
                .publishingAction()
                .log();

        boolean datasetsPublished = false;
        try {
            datasetServiceSupplier.getService().publishDatasetsInCollection(collection);
            datasetsPublished = true;
        } catch (Exception e) {
            logError(e, "Exception updating API dataset to state published")
                    .publishingAction()
                    .collectionName(collection)
                    .collectionId(collection)
                    .log();

            PostMessageField msg = new PostMessageField("Error", e.getMessage(), false);
            collectionAlarm(collection, "Exception updating API dataset to state published", msg);

        } finally {
            saveCollection(collection, datasetsPublished);
        }
        return datasetsPublished;
    }

    private static void saveCollection(Collection collection, boolean publishComplete) {
        // Save any updates to the collection
        logInfo("persisting collection changes to disk")
                .publishingAction()
                .collectionId(collection)
                .addParameter("publishComplete", publishComplete)
                .hostToTransactionID(collection.getDescription().publishTransactionIds)
                .log();
        try {
            collection.save();
        } catch (Exception e) {
            logError(e, "error while attempting to persist collection changes to disk")
                    .publishingAction()
                    .collectionId(collection)
                    .addParameter("publishComplete", publishComplete)
                    .hostToTransactionID(collection.getDescription().publishTransactionIds)
                    .log();
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
