package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

public class Publisher {

    public static boolean Publish(Collection collection, String email) throws IOException {

        boolean publishComplete = false;
        System.out.println("Starting publish process for collection " + collection.description.name);

        long publishStart = System.currentTimeMillis();

        if (!collection.description.approvedStatus) {
            return false;
        }

        Host host = new Host(Configuration.getTheTrainUrl());
        String encryptionPassword = Random.password(100);
        String transactionId = null;
        try {
            transactionId = beginPublish(host, encryptionPassword);
            ExecutorService pool = Executors.newCachedThreadPool();
            List<Future<IOException>> results = new ArrayList<>();

            // Publish each item of content:
            for (String uri : collection.reviewed.uris()) {

                Path source = collection.reviewed.get(uri);
                if (source != null) {
                    results.add(publishFile(host, transactionId, encryptionPassword, uri, source, pool));
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
            Result result = commitPublish(host, transactionId, encryptionPassword);

            if (!result.error) {
                collection.description.AddEvent(new Event(new Date(), EventType.PUBLISHED, email));
            }

            publishComplete = true;

        } catch (IOException e) {

            System.out.println("Exception publishing collection " + collection.description.name + ": " + e.getMessage());
            // If an error was caught, attempt to roll back the transaction:
            if (transactionId != null) {
                System.out.println("Attempting rollback of publishing transaction for collection " + collection.description.name);
                rollbackPublish(host, transactionId, encryptionPassword);
            }
        }

        // Save a published collections log
        collection.save();

        long secondsTaken = (System.currentTimeMillis() - publishStart) / 1000;
        System.out.println("Publish process finished for collection " + collection.description.name
                + " complete=" + publishComplete
                + " time taken=" + secondsTaken + " seconds");

        return publishComplete;
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
    private static Future<IOException> publishFile(final Host host, final String transactionId, final String encryptionPassword, final String uri, final Path source, ExecutorService pool) {
        return pool.submit(new Callable<IOException>() {
            @Override
            public IOException call() throws Exception {
                IOException result = null;
                try (Http http = new Http()) {
                    Endpoint publish = new Endpoint(host, "publish")
                            .setParameter("transactionId", transactionId)
                            .setParameter("encryptionPassword", encryptionPassword)
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
