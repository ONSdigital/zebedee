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


public class Publisher {

    private static final List<Host> theTrainHosts;
    private static final ExecutorService pool = Executors.newFixedThreadPool(20);

    static {
        String[] theTrainUrls = Configuration.getTheTrainUrls();
        theTrainHosts = new ArrayList<>();
        for (String theTrainUrl : theTrainUrls) {
            theTrainHosts.add(new Host(theTrainUrl));
        }
        Runtime.getRuntime().addShutdownHook(new ShutDownPublisherThread(pool));
    }

    public static boolean Publish(Collection collection, String email, CollectionReader collectionReader) throws IOException {
        boolean publishComplete = false;

        // First get the in-memory (within-JVM) lock.
        // This will block attempts to write to the collection during the publishing process
        logInfo("Attempting to lock collection before publish.").addParameter("collectionId", collection.description.id).log();
        Lock writeLock = collection.getWriteLock();
        writeLock.lock();

        long publishStart = System.currentTimeMillis();

        try {
            // First check the state of the collection
            if (collection.description.publishComplete) {
                logInfo("Collection has already been published. Halting publish").collectionId(collection).log();
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
                    logInfo("Collection lock acquired").collectionId(collection).log();

                    collection.path.resolve("publish.lock");

                    logInfo("Starting collection publish process").collectionName(collection).log();


                    if (collection.description.approvalStatus != ApprovalStatus.COMPLETE) {
                        logInfo("Collection cannot be published as it has not been approved").collectionName(collection).log();
                        return false;
                    }

                    publishComplete = PublishFilesToWebsite(collection, email, collectionReader);

                    logInfo("Collectiom publish process finished").collectionName(collection)
                            .timeTaken((System.currentTimeMillis() - publishStart)).log();

                } else {
                    logInfo("Collection is already locked for publishing. Halting publish attempt").collectionId(collection).log();
                }

            } finally {
                // Save any updates to the collection
                collection.save();
            }

        } finally {
            writeLock.unlock();
            logInfo("Collection lock released").collectionId(collection).log();
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
    public static boolean PublishFilesToWebsite(Collection collection, String email, CollectionReader collectionReader) throws IOException {

        boolean publishComplete = false;
        String encryptionPassword = Random.password(100);
        try {
            collection.description.publishStartDate = new Date();
            BeginPublish(collection, encryptionPassword);
            SendManifest(collection, encryptionPassword);
            PublishFilteredCollectionFiles(collection, collectionReader, encryptionPassword);
            publishComplete = CommitPublish(collection, email, encryptionPassword);
            collection.description.publishEndDate = new Date();

        } catch (IOException e) {

            SlackNotification.alarm(String.format("Exception publishing collection: %s: %s", collection.description.name, e.getMessage()));
            logError(e, "Exception publishing collection").collectionName(collection).log();
            // If an error was caught, attempt to roll back the transaction:
            Map<String, String> transactionIds = collection.description.publishTransactionIds;
            if (transactionIds != null && transactionIds.size() > 0) {
                logInfo("Attempting rollback of collection publishing transaction").collectionName(collection).log();
                rollbackPublish(collection.description.publishTransactionIds, encryptionPassword);
            }

        } finally {
            // Save any updates to the collection
            collection.save();
        }

        return publishComplete;
    }

    /**
     * Publish collection files with required filters applied.
     *
     * @param collection
     * @param collectionReader
     * @param encryptionPassword
     * @throws IOException
     */
    public static void PublishFilteredCollectionFiles(Collection collection, CollectionReader collectionReader, String encryptionPassword) throws IOException {
        // We do not want to send versioned files. They have already been taken care of via the manifest.
        // Pass the function to filter files into the publish method.
        Function<String, Boolean> versionedUriFilter = uri -> VersionedContentItem.isVersionedUri(uri);
        Function<String, Boolean> timeseriesUriFilter = uri -> uri.contains("/timeseries/");
        Publisher.PublishCollectionFiles(collection, collectionReader, encryptionPassword, versionedUriFilter, timeseriesUriFilter);
    }

    public static void SendManifest(Collection collection, String encryptionPassword) throws IOException {

        Manifest manifest = Manifest.get(collection);

        long start = System.currentTimeMillis();
        List<Future<IOException>> futures = new ArrayList<>();

        for (Map.Entry<String, String> entry : collection.description.publishTransactionIds.entrySet()) {
            Host theTrainHost = new Host(entry.getKey());
            String transactionId = entry.getValue();

            futures.add(pool.submit(() -> {
                IOException result = null;
                try (Http http = new Http()) {
                    Endpoint publish = new Endpoint(theTrainHost, "CommitManifest")
                            .setParameter("transactionId", transactionId)
                            .setParameter("encryptionPassword", encryptionPassword);

                    Response<Result> response = http.postJson(publish, manifest, Result.class);
                    checkResponse(response);

                } catch (IOException e) {
                    result = e;
                }
                return result;
            }));
        }

        // wait for all results to return, checking if an exception has occurred
        for (Future<IOException> future : futures) {
            try {
                IOException exception = future.get();
                if (exception != null) throw exception;
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error in sendManifest", e);
            }
        }

        logInfo("sendManifest complete").timeTaken(System.currentTimeMillis() - start).log();
    }


    /**
     * Start a new transaction on the train and save the transaction ID to the collection.
     *
     * @param collection
     * @param encryptionPassword
     * @return
     * @throws IOException
     */
    public static Map<String, String> BeginPublish(Collection collection, String encryptionPassword) throws IOException {

        long start = System.currentTimeMillis();

        logInfo("PRE-PUBLISH: Begin transaction").collectionName(collection).log();

        Map<String, String> hostToTransactionId = beginPublish(theTrainHosts, encryptionPassword);
        collection.description.publishTransactionIds = hostToTransactionId;

        collection.save();

        logInfo("PRE-PUBLISH: BeginPublish complete").timeTaken(System.currentTimeMillis() - start).log();

        return hostToTransactionId;
    }

    public static boolean CommitPublish(Collection collection, String email, String encryptionPassword) throws IOException {

        boolean publishComplete = false;
        long start = System.currentTimeMillis();

        logInfo("CommitPublish collectiom start").collectionName(collection).log();
        // If all has gone well so far, commit the publishing transaction:
        boolean success = true;
        for (Result result : commitPublish(collection.description.publishTransactionIds, encryptionPassword)) {
            success &= !result.error;
            collection.description.AddPublishResult(result);
        }

        if (success) {
            Date publishedDate = new Date();
            collection.description.addEvent(new Event(publishedDate, EventType.PUBLISHED, email));
            collection.description.publishComplete = true;
            publishComplete = true;
        }

        logInfo("CommitPublish end").collectionName(collection).timeTaken((System.currentTimeMillis() - start)).log();
        return publishComplete;
    }

    public static void PublishCollectionFiles(
            Collection collection,
            CollectionReader collectionReader,
            String encryptionPassword,
            Function<String, Boolean>... filters
    ) throws IOException {

        List<Future<IOException>> results = new ArrayList<>();
        long start = System.currentTimeMillis();

        logInfo("PublishFiles start").collectionName(collection).log();
        // Publish each item of content:
        for (String uri : collection.reviewed.uris()) {
            if (!shouldBeFiltered(filters, uri)) {
                logInfo("Start PublishFile").collectionId(collection).addParameter("uri", uri).log();
                publishFile(collection, encryptionPassword, pool, results, uri, collectionReader);
            }
        }

        // Check the publishing results:
        for (Future<IOException> result : results) {
            try {
                IOException exception = result.get();
                if (exception != null) throw exception;
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error in file publish", e);
            }
        }
        logInfo("PublishFiles end").collectionName(collection).timeTaken((System.currentTimeMillis() - start)).log();
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

    private static void publishFile(
            Collection collection,
            String encryptionPassword,
            ExecutorService pool,
            List<Future<IOException>> results,
            String uri,
            CollectionReader reader
    ) {
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

            for (Map.Entry<String, String> entry : collection.description.publishTransactionIds.entrySet()) {
                Host theTrainHost = new Host(entry.getKey());
                String transactionId = entry.getValue();
                results.add(publishFile(theTrainHost, transactionId, encryptionPassword, uri, publishUri, zipped, source, reader, pool));
            }
        }
    }

    /**
     * Starts a publishing transaction.
     *
     * @param hosts              The list of target Train {@link Host}s
     * @param encryptionPassword The password used to encrypt files during publishing.
     * @return The new transaction ID.
     * @throws IOException If any errors are encountered in making the request or reported in the {@link com.github.onsdigital.zebedee.json.publishing.Result}.
     */
    private static Map<String, String> beginPublish(List<Host> hosts, String encryptionPassword) throws IOException {
        Map<String, String> hostToTransactionIdMap = new ConcurrentHashMap<>();
        try (Http http = new Http()) {

            List<Future<IOException>> results = new ArrayList<>();

            // submit a beginPublish call for each host to the thread pool.
            for (Host host : hosts) {
                results.add(pool.submit(() -> {
                    IOException result = null;
                    try {
                        logInfo("BeginPublish start").addParameter("host", host.toString()).log();
                        Endpoint begin = new Endpoint(host, "begin").setParameter("encryptionPassword", encryptionPassword);
                        Response<Result> response = http.post(begin, Result.class);
                        checkResponse(response);
                        hostToTransactionIdMap.put(host.toString(), response.body.transaction.id);
                        logInfo("BeginPublish end").addParameter("host", host.toString()).log();
                    } catch (IOException e) {
                        result = e;
                    }

                    return result;
                }));
            }

            // wait for all results to return, checking if an exception has occurred
            for (Future<IOException> result : results) {
                try {
                    IOException exception = result.get();
                    if (exception != null) throw exception;
                } catch (InterruptedException | ExecutionException e) {
                    throw new IOException("Error in BeginPublish", e);
                }
            }
        }
        return hostToTransactionIdMap;
    }

    /**
     * Submits files for asynchronous publishing.
     *
     * @param host               The Train {@link Host}
     * @param transactionId      The transaction to publish to.
     * @param encryptionPassword The password used to encrypt files duing publishing.
     * @param uri                The destination URI.
     * @param source             The data to be published.
     * @param pool               An {@link ExecutorService} to use for asynchronous execution.
     * @return A {@link Future} that will evaluate to {@code null} unless an error occurs in publishing a file, in which case the exception will be returned.
     * @throws IOException
     */
    private static Future<IOException> publishFile(
            final Host host,
            final String transactionId,
            final String encryptionPassword,
            final String uri,
            final String publishUri,
            final boolean zipped,
            final Path source,
            final CollectionReader reader,
            ExecutorService pool
    ) {
        return pool.submit(() -> {
            IOException result = null;
            try (Http http = new Http()) {
                Endpoint publish = new Endpoint(host, "publish")
                        .setParameter("transactionId", transactionId)
                        .setParameter("encryptionPassword", encryptionPassword)
                        .setParameter("zip", Boolean.toString(zipped))
                        .setParameter("uri", publishUri);
                System.out.println("uri = " + uri);
                try (
                        Resource resource = reader.getResource(uri);
                        InputStream dataStream = resource.getData()
                ) {
                    Response<Result> response = http.post(publish, dataStream, source.getFileName().toString(), Result.class);
                    checkResponse(response);
                }
            } catch (IOException e) {
                result = e;
            }
            return result;
        });
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
                    logInfo("CommitPublish start").addParameter("host", host.toString()).log();
                    results.add(endPublish(host, "commit", transactionId, encryptionPassword));
                    logInfo("CommitPublish end").addParameter("host", host.toString()).log();
                } catch (IOException e) {
                    result = e;
                }

                return result;
            }));
        }

        // wait for all results to return, checking if an exception has occurred
        for (Future<IOException> future : futures) {
            try {
                IOException exception = future.get();
                if (exception != null) throw exception;
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error in commitPublish", e);
            }
        }

        return results;
    }

    /**
     * Rolls back a publishing transaction, suppressing any {@link IOException} and printing it out to the console instead.
     *
     * @param transactionIds     The {@link Host}s and transactions we are attempting to publish to.
     * @param encryptionPassword The password used to encrypt files during publishing.
     */
    public static void rollbackPublish(Map<String, String> transactionIds, String encryptionPassword) {
        for (Map.Entry<String, String> entry : transactionIds.entrySet()) {
            Host host = new Host(entry.getKey());
            String transactionId = entry.getValue();
            try {
                endPublish(host, "rollback", transactionId, encryptionPassword);
            } catch (IOException e) {
                logError(e, "Error rolling back publish transaction:").log();
            }
        }
    }

    static Result endPublish(Host host, String endpointName, String transactionId, String encryptionPassword) throws IOException {
        Result result;
        try (Http http = new Http()) {
            Endpoint endpoint = new Endpoint(host, endpointName)
                    .setParameter("transactionId", transactionId)
                    .setParameter("encryptionPassword", encryptionPassword);
            Response<Result> response = http.post(endpoint, Result.class);
            checkResponse(response);
            result = response.body;
        }
        return result;
    }

    static void checkResponse(Response<Result> response) throws IOException {

        if (response.statusLine.getStatusCode() != 200) {
            int code = response.statusLine.getStatusCode();
            String reason = response.statusLine.getReasonPhrase();
            String message = response.body != null ? response.body.message : "";
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
