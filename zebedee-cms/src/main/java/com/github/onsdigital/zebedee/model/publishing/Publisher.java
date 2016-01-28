package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.httpino.*;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import com.github.onsdigital.zebedee.util.ContentTree;
import com.github.onsdigital.zebedee.util.Log;
import com.github.onsdigital.zebedee.util.URIUtils;
import com.github.onsdigital.zebedee.util.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

public class Publisher {

    private static final List<Host> theTrainHosts;
    private static final ExecutorService pool = Executors.newFixedThreadPool(50);

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
        Log.print("Attempting to lock collection before publish: " + collection.description.id);
        Lock writeLock = collection.getWriteLock();
        writeLock.lock();

        long publishStart = System.currentTimeMillis();

        try {
            // First check the state of the collection
            if (collection.description.publishComplete) {
                Log.print("Collection has already been published. Halting publish: " + collection.description.id);
                return publishComplete;
            }

            // Now attempt to get a file (inter-JVM) lock
            // This prevents Staging and Live attempting to
            // publish the same collection at the same time.
            // We specify WRITE so we can get a lock and
            // CREATE to ensure the file is created if it
            // doesn't exist.
            try (FileChannel channel = FileChannel.open(collection.path.resolve(".lock"), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {

                // If the lock can't be acquired, we'll get null:
                FileLock lock = channel.tryLock();
                if (lock != null) {
                    Log.print("Collection lock acquired: " + collection.description.id);

                    collection.path.resolve("publish.lock");

                    Log.print("Starting publish process for collection %s", collection.description.name);


                    if (!collection.description.approvedStatus) {
                        Log.print("The collection %s cannot be published as it has not been approved", collection.description.name);
                        return false;
                    }

                    publishComplete = PublishFilesToWebsite(collection, email, collectionReader);

                    long msTaken = (System.currentTimeMillis() - publishStart);
                    Log.print("Publish process finished for collection %s complete: %s time taken: %dms",
                            collection.description.name,
                            publishComplete,
                            msTaken);

                } else {
                    Log.print("Collection is already locked for publishing. Halting publish attempt on: " + collection.description.id);
                }

            } finally {
                // Save any updates to the collection
                collection.save();
            }

        } finally {
            writeLock.unlock();
            Log.print("Collection lock released: " + collection.description.id);
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

            BeginPublish(collection, encryptionPassword);

            PublishAllCollectionFiles(collection, collectionReader, encryptionPassword);

            publishComplete = CommitPublish(collection, email, encryptionPassword);

        } catch (IOException e) {

            Log.print("Exception publishing collection: %s: %s", collection.description.name, e.getMessage());
            System.out.println(ExceptionUtils.getStackTrace(e));
            // If an error was caught, attempt to roll back the transaction:
            Map<String, String> transactionIds = collection.description.publishTransactionIds;
            if (transactionIds != null && transactionIds.size() > 0) {
                Log.print("Attempting rollback of publishing transaction for collection: " + collection.description.name);
                rollbackPublish(collection.description.publishTransactionIds, encryptionPassword);
            }

        } finally {
            // Save any updates to the collection
            collection.save();
        }

        return publishComplete;
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

        Log.print("BeginPublish start for collection %s", collection.description.name);
        Map<String, String> hostToTransactionId = beginPublish(theTrainHosts, encryptionPassword);
        collection.description.publishTransactionIds = hostToTransactionId;
        Log.print("BeginPublish End for collection %s", collection.description.name);

        collection.save();

        Log.print("BeginPublish Time taken: %sms", (System.currentTimeMillis() - start));

        return hostToTransactionId;
    }

    public static boolean CommitPublish(Collection collection, String email, String encryptionPassword) throws IOException {

        boolean publishComplete = false;
        long start = System.currentTimeMillis();

        Log.print("CommitPublish start for collection %s", collection.description.name);
        // If all has gone well so far, commit the publishing transaction:
        boolean success = true;
        for (Result result : commitPublish(collection.description.publishTransactionIds, encryptionPassword)) {
            success &= !result.error;
            collection.description.AddPublishResult(result);
        }

        if (success) {
            Date publishedDate = new Date();
            collection.description.AddEvent(new Event(publishedDate, EventType.PUBLISHED, email));
            collection.description.publishDate = publishedDate;
            collection.description.publishComplete = true;
            publishComplete = true;
        }

        Log.print("CommitPublish end for collection %s: Time taken: %sms", collection.description.name, (System.currentTimeMillis() - start));
        return publishComplete;
    }

    public static void PublishAllCollectionFiles(Collection collection, CollectionReader collectionReader, String encryptionPassword) throws IOException {
        List<Future<IOException>> results = new ArrayList<>();
        long start = System.currentTimeMillis();

        Log.print("PublishFiles start");
        // Publish each item of content:
        for (String uri : collection.reviewed.uris()) {
            //Log.print("Start PublishFile: %s", uri);
            publishFile(collection, encryptionPassword, pool, results, uri, collectionReader);
            //Log.print("End PublishFile: %s", uri);
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

        Log.print("PublishFiles end: Time taken: %sms", (System.currentTimeMillis() - start));
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

            unzipTimeseries(collection, collectionReader, zebedee);
            copyFilesToMaster(zebedee, collection, collectionReader);

            Log.print("Reindexing search");
            reindexSearch(collection);

            // move collection files to archive
            Path collectionJsonPath = moveCollectionToArchive(zebedee, collection, collectionReader);
            //zebedee.publishedCollections.add(collectionJsonPath);

            // send a slack success message
            sendSlackMessageForCollection(collectionJsonPath);

            if (!skipVerification) {
                // add to published collections list
                indexPublishReport(zebedee, collectionJsonPath, collectionReader);
            }

            collection.delete();
            ContentTree.dropCache();
            return true;
        } catch (Exception exception) {
            Log.print("An error occurred during the publish cleanupon collection %s: %s", collection.description.name, exception.getMessage());
            ExceptionUtils.printRootCauseStackTrace(exception);
        }

        return false;
    }

    private static void indexPublishReport(final Zebedee zebedee, final Path collectionJsonPath, final CollectionReader collectionReader) {
        pool.submit(() -> {
            Log.print("Indexing publish report");
            PublishedCollection publishedCollection = zebedee.publishedCollections.add(collectionJsonPath);
            if (Configuration.isVerificationEnabled()) {
                zebedee.verificationAgent.submitForVerification(publishedCollection, collectionJsonPath, collectionReader);
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
        Log.print("Unzipping files if required to move to master.");
        for (String uri : collection.reviewed.uris()) {
            Path source = collection.reviewed.get(uri);
            if (source != null) {
                if (source.getFileName().toString().equals("timeseries-to-publish.zip")) {
                    String publishUri = StringUtils.removeStart(StringUtils.removeEnd(uri, "-to-publish.zip"), "/");
                    Path publishPath = zebedee.published.path.resolve(publishUri);
                    Log.print("Unzipping %s to %s", source.toString(), publishPath.toString());

                    Resource resource = collectionReader.getResource(uri);
                    ZipUtils.unzip(resource.getData(), publishPath.toString());
                }
            }
        }
    }

    /**
     * Send a slack message containing collection publication information
     *
     * @param collectionJsonPath
     */
    private static void sendSlackMessageForCollection(Path collectionJsonPath) {
        String slackBaseUri = "https://slack.com/api/chat.postMessage";
        final Host slackHost = new Host(slackBaseUri);

        // publishbot requires a Slack token (which is generated for a specific team) and a channel name to publish to
        String slackToken = System.getenv("publishbot_token");
        String slackChannel = System.getenv("publishbot_channel");
        if (slackToken == null || slackChannel == null) {
            return;
        }

        try (InputStream input = Files.newInputStream(collectionJsonPath)) {
            PublishedCollection publishedCollection = Serialiser.deserialise(input,
                    PublishedCollection.class);

            // set up further slack variables
            ExecutorService pool = Executors.newFixedThreadPool(1);
            String slackUsername = "PublishBot";
            String slackEmoji = ":chart_with_upwards_trend:";

            // get the message for the publication
            String slackMessage = publicationMessage(publishedCollection);

            // send the message
            Future<Exception> exceptionFuture = sendSlackMessage(slackHost, slackToken, slackChannel, slackUsername, slackEmoji, slackMessage, pool);
            exceptionFuture.get();
        } catch (Exception e) {
            e.printStackTrace();
            Log.print("Failed to slack message for published collection with path %s", collectionJsonPath.toString());
        }
    }

    private static Future<Exception> sendSlackMessage(
            final Host host,
            final String token, final String channel,
            final String userName, final String emoji,
            final String text,
            ExecutorService pool
    ) {
        return pool.submit(new Callable<Exception>() {
            @Override
            public Exception call() throws Exception {
                Exception result = null;
                try (Http http = new Http()) {
                    Endpoint slack = new Endpoint(host, "")
                            .setParameter("token", token)
                            .setParameter("username", userName)
                            .setParameter("channel", channel)
                            .setParameter("icon_emoji", emoji)
                            .setParameter("text", StringEscapeUtils.escapeHtml(text));
                    http.getFile(slack);
                } catch (Exception e) {
                    result = e;
                }
                return result;
            }
        });
    }

    private static String publicationMessage(PublishedCollection publishedCollection) throws ParseException {
        Result result = publishedCollection.publishResults.get(0);
        String startDate = result.transaction.startDate;
        String endDate = result.transaction.endDate;

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Date startDateTime = parser.parse(startDate);
        Date endDateTime = parser.parse(endDate);
        String timeTaken = String.format("%.1f", (endDateTime.getTime() - startDateTime.getTime()) / 1000.0);

        String exampleUri = "";
        for (UriInfo info : result.transaction.uriInfos) {
            if (info.uri.endsWith("data.json")) {
                exampleUri = info.uri.substring(0, info.uri.length() - "data.json".length());
                break;
            }
        }

        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        String message = "Collection " + publishedCollection.name +
                " was published at " + format.format(publishedCollection.publishDate) +
                " with " + result.transaction.uriInfos.size() + " files " +
                " in " + timeTaken + " seconds. Example file: http://beta.ons.gov.uk" + exampleUri;

        return message;
    }

    private static void reindexSearch(Collection collection) throws IOException {

        Log.print("Reindexing search for collection %s", collection.description.name);
        try {

            long start = System.currentTimeMillis();

            List<String> uris = collection.reviewed.uris("*data.json");
            for (String uri : uris) {
                if (isIndexedUri(uri)) {
                    String contentUri = URIUtils.removeLastSegment(uri);
                    reIndexPublishingSearch(contentUri);
                }
            }

            Log.print("Time taken re-indexing search %sms", (System.currentTimeMillis() - start));

        } catch (Exception exception) {
            Log.print("An error occurred during the search reindex of collection %s, %s", collection.description.name, exception.getMessage());
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
                Log.print("Exception reloading search index:");
                ExceptionUtils.printRootCauseStackTrace(e);
            }
        });
    }

    public static void copyFilesToMaster(Zebedee zebedee, Collection collection, CollectionReader collectionReader) throws IOException, ZebedeeException {

        Log.print("Moving files from collection into master for collection: " + collection.description.name);
        // Move each item of content:
        for (String uri : collection.reviewed.uris()) {
            Path destination = zebedee.published.toPath(uri);
            Resource resource = collectionReader.getResource(uri);
            FileUtils.copyInputStreamToFile(resource.getData(), destination.toFile());
        }
    }

    public static Path moveCollectionToArchive(Zebedee zebedee, Collection collection, CollectionReader collectionReader) throws IOException, ZebedeeException {

        Log.print("Moving collection files to archive for collection: " + collection.description.name);
        String filename = PathUtils.toFilename(collection.description.name);
        Path collectionJsonSource = zebedee.collections.path.resolve(filename + ".json");
        Path collectionFilesSource = collection.reviewed.path;
        Path logPath = zebedee.publishedCollections.path;

        if (!Files.exists(logPath)) {
            Files.createDirectory(logPath);
        }

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
        String directoryName = format.format(date) + "-" + filename;
        Path collectionFilesDestination = logPath.resolve(directoryName);
        Path collectionJsonDestination = logPath.resolve(directoryName + ".json");

        Log.print("Moving collection json from %s to %s", collectionJsonSource, collectionJsonDestination);
        Files.copy(collectionJsonSource, collectionJsonDestination);

        Log.print("Moving collection files from %s to %s", collectionFilesSource, collectionFilesDestination);
        for (String uri : collection.reviewed.uris()) {
            Resource resource = collectionReader.getResource(uri);
            File destination = collectionFilesDestination.resolve(URIUtils.removeLeadingSlash(uri)).toFile();
            FileUtils.copyInputStreamToFile(resource.getData(), destination);
        }

        return collectionJsonDestination;
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
        Map<String, String> hostToTransactionIdMap = new HashMap<>();
        try (Http http = new Http()) {

            List<Future<IOException>> results = new ArrayList<>();

            // submit a beginPublish call for each host to the thread pool.
            for (Host host : hosts) {
                results.add(pool.submit(() -> {
                    IOException result = null;
                    try {
                        Log.print("BeginPublish start for host: %s", host.toString());
                        Endpoint begin = new Endpoint(host, "begin").setParameter("encryptionPassword", encryptionPassword);
                        Response<Result> response = http.post(begin, Result.class);
                        checkResponse(response);
                        hostToTransactionIdMap.put(host.toString(), response.body.transaction.id);
                        Log.print("BeginPublish end for host: %s", host.toString());
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

                Resource resource = reader.getResource(uri);
                Response<Result> response = http.post(publish, resource.getData(), source.getFileName().toString(), Result.class);
                checkResponse(response);

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
                    Log.print("CommitPublish start for host %s", host.toString());
                    results.add(endPublish(host, "commit", transactionId, encryptionPassword));
                    Log.print("CommitPublish end for host %s", host.toString());
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
                System.out.println("Error rolling back publish transaction:");
                System.out.println(ExceptionUtils.getStackTrace(e));
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
