package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import com.github.onsdigital.zebedee.util.ContentTree;
import com.github.onsdigital.zebedee.util.Log;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

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

    private static final Host websiteHost = new Host(Configuration.getWebsiteUrl());
    private static final Host theTrainHost = new Host(Configuration.getTheTrainUrl());


    public static boolean Publish(Zebedee zebedee, Collection collection, String email) throws IOException {
        boolean publishComplete = false;

        // First get the in-memory (within-JVM) lock.
        // This will block attempts to write to the collection during the publishing process
        Log.print("Attempting to lock collection before publish: " + collection.description.id);
        Lock writeLock = collection.getWriteLock();
        writeLock.lock();

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
                    long publishStart = System.currentTimeMillis();

                    if (!collection.description.approvedStatus) {
                        Log.print("The collection %s cannot be published as it has not been approved", collection.description.name);
                        return false;
                    }

                    publishComplete = PublishFilesToWebsite(collection, email);

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

        if (publishComplete) {
            onPublishComplete(zebedee, collection);
        }

        return publishComplete;
    }

    /**
     * Submit all files in the collection to the train destination - the website.
     *
     * @param collection
     * @param email
     * @return
     * @throws IOException
     */
    private static boolean PublishFilesToWebsite(Collection collection, String email) throws IOException {

        boolean publishComplete = false;
        String encryptionPassword = Random.password(100);
        try {
            collection.description.publishTransactionId = beginPublish(theTrainHost, encryptionPassword);
            collection.save();
            ExecutorService pool = Executors.newFixedThreadPool(50);
            List<Future<IOException>> results = new ArrayList<>();

            // Publish each item of content:
            for (String uri : collection.reviewed.uris()) {
                Path source = collection.reviewed.get(uri);
                if (source != null) {
                    boolean zipped = false;
                    if (source.getFileName().toString().equals("timeseries")) {
                        zipped = true;
                    }

                    results.add(publishFile(theTrainHost, collection.description.publishTransactionId, encryptionPassword, uri, zipped, source, pool));
                }

                // Add an event to the event log
                collection.AddEvent(uri, new Event(new Date(), EventType.PUBLISHED, email));
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

            // If all has gone well so far, commit the publishing transaction:
            Result result = commitPublish(theTrainHost, collection.description.publishTransactionId, encryptionPassword);

            if (!result.error) {
                Date publishedDate = new Date();
                collection.description.AddEvent(new Event(publishedDate, EventType.PUBLISHED, email));
                collection.description.publishDate = publishedDate;
                collection.description.publishComplete = true;
                publishComplete = true;
            }

            collection.description.AddPublishResult(result);

        } catch (IOException e) {

            Log.print("Exception publishing collection: %s: %s", collection.description.name, e.getMessage());
            System.out.println(ExceptionUtils.getStackTrace(e));
            // If an error was caught, attempt to roll back the transaction:
            if (collection.description.publishTransactionId != null) {
                Log.print("Attempting rollback of publishing transaction for collection: " + collection.description.name);
                rollbackPublish(theTrainHost, collection.description.publishTransactionId, encryptionPassword);
            }

        } finally {
            // Save any updates to the collection
            collection.save();
        }

        return publishComplete;
    }

    /**
     * Do tasks required after a publish takes place.
     *
     * @param zebedee
     * @param collection
     * @return
     * @throws IOException
     */
    private static boolean onPublishComplete(Zebedee zebedee, Collection collection) throws IOException {

        try {
            copyFilesToMaster(zebedee, collection);

            // move collection files to archive
            Path collectionJsonPath = moveCollectionToArchive(zebedee, collection);

            zebedee.publishedCollections.add(collectionJsonPath);

            // send a slack success message
            sendSlackMessageForCollection(collectionJsonPath);

            Log.print("Reindexing search");
            reindexSearch(collection);

            collection.delete();
            ContentTree.dropCache();

            return true;
        } catch (Exception exception) {
            Log.print("An error occurred during the publish cleanupon collection %s: %s", collection.description.name, exception.getMessage());
            ExceptionUtils.printRootCauseStackTrace(exception);
        }

        return false;
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
        if (slackToken == null || slackChannel == null) { return; }

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
            ExecutorService pool) {
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
        for (UriInfo info: result.transaction.uriInfos) {
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

        try {
            List<String> uris = collection.reviewed.uris("*data.json");
            for (String uri : uris) {
                String contentUri = URIUtils.removeLastSegment(uri);
                reIndexPublishingSearch(contentUri);
                reIndexWebsiteSearch(contentUri);
            }
        } catch (Exception exception) {
            Log.print("An error occurred during the search reindex of collection %s, %s", collection.description.name, exception.getMessage());
            ExceptionUtils.printRootCauseStackTrace(exception);
        }
    }


    /**
     * Post to the website to reindex search.
     */
    private static void reIndexWebsiteSearch(String uri) {
        Log.print("Reindexing website search.");
        try (Http http = new Http()) {

            Endpoint begin = new Endpoint(websiteHost, "reindex").setParameter("key", Configuration.getReindexKey()).setParameter("uri", uri);
            Response<String> response = http.post(begin, String.class);
            Log.print("Website reindex response: %s", response.body);

        } catch (Exception e) {
            Log.print("Exception reloading website search index:");
            ExceptionUtils.printRootCauseStackTrace(e);
        }
    }

    private static void reIndexPublishingSearch(String uri) throws IOException {
        try {
            Indexer.getInstance().reloadContent(uri);
        } catch (Exception e) {
            Log.print("Exception reloading search index:");
            ExceptionUtils.printRootCauseStackTrace(e);
        }
    }

    public static void copyFilesToMaster(Zebedee zebedee, Collection collection) throws IOException {

        Log.print("Moving files from collection into master for collection: " + collection.description.name);
        // Move each item of content:
        for (String uri : collection.reviewed.uris()) {

            Path source = collection.reviewed.get(uri);
            if (source != null) {
                Path destination = zebedee.published.toPath(uri);
                PathUtils.copyFilesInDirectory(source, destination);
            }
        }
    }

    public static Path moveCollectionToArchive(Zebedee zebedee, Collection collection) throws IOException {

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
        FileUtils.moveDirectoryToDirectory(collectionFilesSource.toFile(), collectionFilesDestination.toFile(), true);

        return collectionJsonDestination;
    }

    /**
     * Starts a publishing transaction.
     *
     * @param host               The Train {@link Host}
     * @param encryptionPassword The password used to encrypt files during publishing.
     * @return The new transaction ID.
     * @throws IOException If any errors are encountered in making the request or reported in the {@link com.github.onsdigital.zebedee.json.publishing.Result}.
     */
    static String beginPublish(Host host, String encryptionPassword) throws IOException {
        String result = null;
        try (Http http = new Http()) {
            Endpoint begin = new Endpoint(host, "begin").setParameter("encryptionPassword", encryptionPassword);
            Response<Result> response = http.post(begin, Result.class);
            checkResponse(response);
            result = response.body.transaction.id;
        }
        return result;
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
            final boolean zipped,
            final Path source,
            ExecutorService pool) {
        return pool.submit(new Callable<IOException>() {
            @Override
            public IOException call() throws Exception {
                IOException result = null;
                try (Http http = new Http()) {
                    Endpoint publish = new Endpoint(host, "publish")
                            .setParameter("transactionId", transactionId)
                            .setParameter("encryptionPassword", encryptionPassword)
                            .setParameter("zip", Boolean.toString(zipped))
                            .setParameter("uri", uri);
                    Response<Result> response = http.postFile(publish, source, Result.class);
                    checkResponse(response);
                } catch (IOException e) {
                    result = e;
                }
                return result;
            }
        });
    }

    /**
     * Commits a publishing transaction.
     *
     * @param host               The Train {@link Host}
     * @param transactionId      The transaction to publish to.
     * @param encryptionPassword The password used to encrypt files during publishing.
     * @return The {@link Result} returned by The Train
     * @throws IOException If any errors are encountered in making the request or reported in the {@link Result}.
     */
    static Result commitPublish(Host host, String transactionId, String encryptionPassword) throws IOException {
        return endPublish(host, "commit", transactionId, encryptionPassword);
    }

    /**
     * Rolls back a publishing transaction, suppressing any {@link IOException} and printing it out to the console instead.
     *
     * @param host               The Train {@link Host}
     * @param transactionId      The transaction to publish to.
     * @param encryptionPassword The password used to encrypt files during publishing.
     */
    static void rollbackPublish(Host host, String transactionId, String encryptionPassword) {
        try {
            endPublish(host, "rollback", transactionId, encryptionPassword);
        } catch (IOException e) {
            System.out.println("Error rolling back publish transaction:");
            System.out.println(ExceptionUtils.getStackTrace(e));
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
}
