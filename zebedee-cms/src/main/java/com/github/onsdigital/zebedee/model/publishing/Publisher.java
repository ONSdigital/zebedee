package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.json.publishing.request.Manifest;
import com.github.onsdigital.zebedee.logging.CMSLogEvent;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.model.publishing.verify.HashVerifier;
import com.github.onsdigital.zebedee.model.publishing.verify.HashVerifierImpl;
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

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
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
        boolean success = false;

        publishFilteredCollectionFiles(collection, collectionReader);

        if (CMSFeatureFlags.cmsFeatureFlags().isVerifyPublishEnabled()) {
            info().data("feature", "ENABLE_VERIFY_PUBLISH_CONTENT").log("feature enabled verifying publishing content");

            HashVerifier hashVerifier = HashVerifierImpl.getInstance();
            hashVerifier.verifyTransactionContent(collection, collectionReader);
        }

        // TODO - feels like we should check/return here if unsuccessful?
        success = commitPublish(collection, email);

        // FIXME CMD feature
        if (cmsFeatureFlags().isEnableDatasetImport()) {
            success &= publishDatasets(collection);
        }

        info().data("milliseconds", collection.getPublishTimeMilliseconds())
                .data("publishComplete", success)
                .data("publishing", true)
                .data("collectionId", collection.getDescription().getId())
                .log("collection publish time");

        return success;
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
        info().data("publishing", true).data("collectionId", collection.getDescription().getId())
                .log("attempting to lock collection for publish");

        Lock writeLock = collection.getWriteLock();
        writeLock.lock();

        String collectionId = collection.getDescription().getId();

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
                    info().data("publishing", true).data("collectionId", collectionId)
                            .log("collection lock acquired");

                    if (!isApproved(collection)) {
                        return false;
                    }

                    publishComplete = publishFilesToWebsite(collection, email, collectionReader);

                    info().data("publishing", true).data("collectionId", collectionId)
                            .data("timeTaken", (System.currentTimeMillis() - publishStart))
                            .log("collection publish process completed");

                } else {
                    info().data("publishing", true).data("collectionId", collectionId)
                            .log("collection is already locked for publishAction, halting publish attempt");
                }

            } finally {
                saveCollection(collection, publishComplete);
            }

        } finally {
            writeLock.unlock();
            info().data("collectionId", collectionId).log("collection lock released");
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
            info().data("publishing", true).data("collectionId", collection.getDescription().getId())
                    .log("collection cannot be published as it has not been approved");
            return false;
        }
        return true;
    }

    /**
     * Return true if collection is already marked as publish completed, return false otherwise
     */
    private static boolean isPublished(Collection collection) {
        if (collection.getDescription().publishComplete) {
            info().data("publishing", true).data("collectionId", collection.getDescription().getId())
                    .log("collection has already been published, halting publish");
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
    public static boolean publishFilesToWebsite(Collection collection, String email, CollectionReader collectionReader)
            throws IOException {
        boolean publishComplete = false;

        try {
            collection.getDescription().publishStartDate = new Date();

            executePrePublish(collection);
            publishComplete = executePublish(collection, collectionReader, email);

            collection.getDescription().publishEndDate = new Date();
        } catch (Exception ex) {
            handlePublishingException(ex, collection);
        } finally {
            // Save any updates to the collection
            saveCollection(collection, publishComplete);
        }
        return publishComplete;
    }

    private static void handlePublishingException(Exception ex, Collection collection) {
        PostMessageField msg = new PostMessageField("Error", ex.getMessage(), false);
        collectionAlarm(collection, "Exception publishAction collection", msg);

        CMSLogEvent err = error().data("publishing", true).collectionID(collection);

        // If an error was caught, attempt to roll back the transaction:
        Map<String, String> transactionIds = collection.getDescription().getPublishTransactionIds();
        if (transactionIds != null && transactionIds.size() > 0) {
            err.data("hostToTransactionID", transactionIds)
                    .exception(ex)
                    .log("publish collection error, attempting to rollback collection");

            rollbackPublish(collection);
        } else {
            err.exception(ex).log("publish collection error. Unable rollback as no transaction IDs found for collection");
        }
    }

    public static Map<String, String> createPublishingTransactions(Collection collection)
            throws IOException {
        long start = System.currentTimeMillis();
        Map<String, String> hostToTransactionIDMap = new ConcurrentHashMap<>();
        List<Future<IOException>> results = new ArrayList<>();

        String collectionId = collection.getDescription().getId();

        for (Host host : theTrainHosts) {
            results.add(pool.submit(() -> {
                IOException result = null;
                try (Http http = new Http()) {
                    info().data("publishing", true).data("trainHost", host).data("collectionId", collectionId).
                            log("creating publish transaction for collection");

                    Endpoint begin = new Endpoint(host, BEGIN_ENDPOINT);

                    Response<Result> response = http.post(begin, Result.class);
                    checkResponse(response, null, begin, collection.getDescription().getId());
                    hostToTransactionIDMap.put(host.toString(), response.body.transaction.id);
                } catch (IOException e) {
                    Map<String, String> transactionIdMap = collection.getDescription().getPublishTransactionIds();
                    error().data("publishing", true).data("trainHost", host).data("collectionId", collectionId).
                            logException(e, "error while attempting to create new transactions for collection");

                    if (transactionIdMap != null && !transactionIdMap.isEmpty()) {
                        warn().data("publishing", true).data("trainHost", host).data("collectionId", collectionId).
                                log("clearing existing transactionIDs from collection");

                        collection.getDescription().getPublishTransactionIds().clear();
                    }
                    result = e;
                }
                return result;
            }));
        }

        checkFutureResults(results, "error creating publishAction transaction");
        collection.getDescription().setPublishTransactionIds(hostToTransactionIDMap);
        collection.save();

        info().data("publishing", true).data("collectionId", collectionId)
                .data("hostToTransactionID", hostToTransactionIDMap)
                .data("timeTaken", System.currentTimeMillis() - start)
                .log("successfully created publish transactions for collection");

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
        for (String uri : collection.getReviewed().uris()) {
            if (!shouldBeFiltered(filters, uri)) {
                //publishFile(collection, encryptionPassword, results, uri, collectionReader);

                Path source = collection.getReviewed().get(uri);
                if (source != null) {
                    boolean zipped = false;
                    String publishUri = uri;

                    // if we have a recognised compressed file - set the zip header and set the correct uri so that the files
                    // are unzipped to the correct place.
                    if (source.getFileName().toString().equals("timeseries-to-publish.zip")) {
                        zipped = true;
                        publishUri = StringUtils.removeEnd(uri, "-to-publish.zip");
                    }

                    for (Map.Entry<String, String> entry : collection.getDescription().getPublishTransactionIds().entrySet()) {
                        Host theTrainHost = new Host(entry.getKey());
                        String transactionId = entry.getValue();

                        results.add(publishFile(collection.getDescription().getId(), theTrainHost,
                                transactionId, uri, publishUri, zipped, source, collectionReader));
                    }
                }
            }
        }

        checkFutureResults(results, "error while attempting to publish file");

        info().data("publishing", true).data("collectionId", collection.getDescription().getId())
                .data("hostToTransactionID", collection.getDescription().getPublishTransactionIds())
                .data("timeTaken", (System.currentTimeMillis() - start)).log("successfully sent all publish file requests to the train");
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
                    info().data("publishing", true).data("collectionId", collectionID)
                            .data("transactionId", transactionId)
                            .data("trainHost", host)
                            .data(URI_PARAM, uri).data("isZip", zipped)
                            .log("sending publish collection file request to train host");

                    Response<Result> response = http.post(publish, dataStream, source.getFileName().toString(), Result.class);
                    checkResponse(response, transactionId, publish, collectionID);
                }
            } catch (IOException e) {

                error().data("publishing", true).data("collectionId", collectionID)
                        .data("transactionId", transactionId)
                        .data("trainHost", host)
                        .data(URI_PARAM, uri).data("isZip", zipped)
                        .logException(e, "error while sending publish file request to train host");
                result = e;
            }
            return result;
        });
    }

    public static void sendManifest(Collection collection) throws IOException {
        Manifest manifest = Manifest.get(collection);
        List<Future<IOException>> futures = new ArrayList<>();
        long start = System.currentTimeMillis();

        String collectionId = collection.getDescription().getId();

        for (Map.Entry<String, String> entry : collection.getDescription().getPublishTransactionIds().entrySet()) {
            Host theTrainHost = new Host(entry.getKey());
            String transactionId = entry.getValue();

            futures.add(pool.submit(() -> {
                IOException result = null;
                try (Http http = new Http()) {
                    Endpoint publish = new Endpoint(theTrainHost, SEND_MANIFEST_ENDPOINT)
                            .setParameter(TRANSACTION_ID_PARAM, transactionId);

                    info().data("publishing", true).data("trainHost", theTrainHost).data("collectionId", collectionId)
                            .log("sending publish manifest to train host");

                    Response<Result> response = http.postJson(publish, manifest, Result.class);
                    checkResponse(response, transactionId, publish, collection.getDescription().getId());

                } catch (IOException e) {

                    error().data("publishing", true).data("collectionId", collectionId)
                            .data("trainHost", theTrainHost).data("transactionId", transactionId)
                            .logException(e, "unexpected error while attempting to send publish manifest to train host");
                    result = e;
                }
                return result;
            }));
        }

        checkFutureResults(futures, "error sending publish manifest");

        info().data("publishing", true).data("collectionId", collection.getDescription().getId())
                .data("hostToTransactionId", collection.getDescription().getPublishTransactionIds())
                .data("timeTaken", System.currentTimeMillis() - start)
                .log("successfully sent publish manifest for collection to train hosts");
    }

    public static boolean commitPublish(Collection collection, String email) throws IOException {
        long start = System.currentTimeMillis();

        // If all has gone well so far, commit the publishAction transaction:
        boolean isSuccess = true;
        for (Result result : commitPublish(collection.getDescription().getPublishTransactionIds())) {
            isSuccess &= !result.error;
            collection.getDescription().AddPublishResult(result);
        }

        if (isSuccess) {
            Date publishedDate = new Date();
            collection.getDescription().addEvent(new Event(publishedDate, EventType.PUBLISHED, email));
            collection.getDescription().publishComplete = true;

            info().data("publishing", true).data("collectionId", collection.getDescription().getId())
                    .data("timeTaken", System.currentTimeMillis() - start)
                    .log("commit publish completed successfully");

            return true;
        } else {
            warn().data("publishing", true).data("collectionId", collection.getDescription().getId())
                    .data("timeTaken", System.currentTimeMillis() - start)
                    .log("commit publish was unsuccessfully");
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

                    info().data("publishing", true).data("transactionId", transactionId).data("trainHost", host)
                            .log("creating commit publish transaction tasks");

                    Callable<Result> task = () -> {
                        try {
                            info().data("publishing", true).data("transactionId", transactionId)
                                    .data("trainHost", host).log("sending commit transaction request to train host");

                            try (Http http = new Http()) {
                                Endpoint endpoint = new Endpoint(host, COMMIT_ENDPOINT)
                                        .setParameter(TRANSACTION_ID_PARAM, transactionId);

                                Response<Result> response = http.post(endpoint, Result.class);
                                checkResponse(response, transactionId, endpoint, null);
                                return response.body;
                            }
                        } catch (Exception e) {
                            error().data("publishing", true).data("trainHost", host).data("transactionId", transactionId)
                                    .logException(e, "error while sending commit transaction request to train host");
                            throw new IOException(e);
                        }
                    };
                    return task;
                }).collect(Collectors.toList());

        info().data("publishing", true).data("count", commitTasks.size()).log("submitting commit publish tasks");

        List<Future<Result>> futures = new ArrayList<>();
        try {
            futures = pool.invokeAll(commitTasks);
        } catch (Exception e) {
            error().data("publishing", true).logException(e, "error invoking commit tasks");
            throw new IOException(e);
        }

        info().data("publishing", true).log("checking commit publish tasks results");

        List<Result> results = new ArrayList<>();
        for (Future<Result> resultFuture : futures) {
            try {
                Result r = resultFuture.get();
                if (r == null) {
                    throw new IOException("commit publish result was null");
                }

                info().data("publishing", true).data("transactionId", r.transaction.id)
                        .data("transactionId", r.transaction.id)
                        .log("adding commit publish result future to list of results");

                results.add(r);
            } catch (Exception e) {
                error().data("publishing", true).logException(e, "commit tranaction future throw unexpected exception");
                throw new IOException(e);
            }
        }

        info().data("publishing", true).data("hostToTransactionId", transactionIds).log("successfully committed publishAction transaction");

        return results;
    }

    /**
     * Rolls back a publishAction transaction, suppressing any {@link IOException} and printing it out to the console instead.
     *
     * @param collection the collection to roll back
     */
    public static void rollbackPublish(Collection collection) {
        for (Map.Entry<String, String> entry : collection.getDescription().getPublishTransactionIds().entrySet()) {
            Host host = new Host(entry.getKey());

            String transactionId = entry.getValue();
            String collectionId = collection.getDescription().getId();

            try (Http http = new Http()) {
                Endpoint endpoint = new Endpoint(host, ROLLBACK_ENDPOINT)
                        .setParameter(TRANSACTION_ID_PARAM, transactionId);

                warn().data("publishing", true).data("collectionId", collectionId)
                        .data("hostToTransactionId", collection.getDescription().getPublishTransactionIds())
                        .log("sending rollback transaction request for collection");

                Response<Result> response = http.post(endpoint, Result.class);
                checkResponse(response, transactionId, endpoint, null);

                info().data("publishing", true).data("collectionId", collectionId)
                        .data("hostToTransactionId", collection.getDescription().getPublishTransactionIds())
                        .log("publish rollback request was successful");

            } catch (IOException e) {
                error().data("publishing", true).data("collectionId", collectionId)
                        .data("transactionId", transactionId)
                        .logException(e, "error rolling back publish transaction");
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

            IOException io = new IOException("Error in request: " + code + " " + reason + " " + message);

            error().data("publishing", true).data("transactionId", TransactionID)
                    .data("collectionId", collectionID)
                    .data("trainHost", uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort())
                    .data("endpoint", endpoint.url().getPath())
                    .data("statusCode", code)
                    .data("reason", reason)
                    .data("message", message)
                    .logException(io, "request was unsuccessful");
            throw io;

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

        String collectionId = collection.getDescription().getId();

        info().data("collectionId", collectionId).data("publishing", true)
                .log("publishing api datasets for collection");

        boolean datasetsPublished = false;
        try {
            datasetServiceSupplier.getService().publishDatasetsInCollection(collection);
            datasetsPublished = true;
        } catch (Exception e) {

            error().data("collectionId", collectionId).data("publishing", true)
                    .logException(e, "Exception updating API dataset to state published");

            PostMessageField msg = new PostMessageField("Error", e.getMessage(), false);
            collectionAlarm(collection, "Exception updating API dataset to state published", msg);

        } finally {
            saveCollection(collection, datasetsPublished);
        }
        return datasetsPublished;
    }

    private static void saveCollection(Collection collection, boolean publishComplete) {
        // Save any updates to the collection

        String collectionId = collection.getDescription().getId();

        info().data("publishing", true).data("publishComplete", publishComplete).data("collectionId", collectionId)
                .data("hostToTransactionId", collection.getDescription().getPublishTransactionIds())
                .log("persisting collection changes to disk");

        try {
            collection.save();
        } catch (Exception e) {

            error().data("publishing", true).data("collectionId", collectionId).data("publishComplete", publishComplete)
                    .data("hostToTransactionId", collection.getDescription().getPublishTransactionIds())
                    .logException(e, "error while attempting to persist collection changes to disk");
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
