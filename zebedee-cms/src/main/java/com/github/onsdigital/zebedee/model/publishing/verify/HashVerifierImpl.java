package com.github.onsdigital.zebedee.model.publishing.verify;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.model.publishing.client.PublishingClient;
import com.github.onsdigital.zebedee.model.publishing.client.PublishingClientImpl;
import com.github.onsdigital.zebedee.reader.CollectionReader;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Provides functionality for verifying the content send the publishing API was received correctly.
 */
public class HashVerifierImpl implements HashVerifier {

    private static final ExecutorService pool = Executors.newFixedThreadPool(50);
    private static HashVerifier instance = null;


    private PublishingClient publishingClient;

    /**
     * Construct a new instance using the default values.
     */
    HashVerifierImpl() {
        this(new PublishingClientImpl());
    }

    /**
     * Construct a new instance using the {@link PublishingClient} provided.
     */
    HashVerifierImpl(PublishingClient publishingClient) {
        this.publishingClient = publishingClient;
    }

    /**
     * Verify the content sent to the publshing APIs was received correctly.
     *
     * @param collection the collection to verify.
     * @param reader     a {@link CollectionReader} used read the collection content.
     * @throws HashVerificationException thrown for any error while attempting to verify the content.
     */
    public void verifyTransactionContent(Collection collection, CollectionReader reader) throws HashVerificationException {
        validateParams(collection, reader);
        List<Callable<Boolean>> tasks = createVerifyTasks(collection, reader);
        List<Future<Boolean>> verifyResults = executeVerifyTasks(tasks);
        checkVerifyResults(verifyResults);
    }

    private void validateParams(Collection collection, CollectionReader reader) {
        requireNotNull(collection, "collection required but was null");
        requireNotNull(reader, "collection reader required but was null");

        CollectionDescription desc = collection.getDescription();
        requireNotNull(desc, "collection.description required but was null");

        Map<String, String> hostTransactionMap = desc.getPublishTransactionIds();
        requireNotNull(hostTransactionMap, "description.publishingTransactionIds required but was null");

        if (hostTransactionMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "error verifying transaction content: host-to-transactionId mapping expected but none found");
        }
    }

    private <T> void requireNotNull(T t, String message) {
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private List<Callable<Boolean>> createVerifyTasks(Collection collection, CollectionReader reader) {
        Map<String, String> hostTransactionIdMap = collection.getDescription().getPublishTransactionIds();
        List<String> urisToVerify = getCollectionUrisToVerify(collection);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (Map.Entry<String, String> hostTransactionMapping : hostTransactionIdMap.entrySet()) {
            String host = hostTransactionMapping.getKey();
            String transactionId = hostTransactionMapping.getValue();

            tasks.addAll(createVerifyTasksForHost(collection.getId(), reader, host, transactionId, urisToVerify));
        }

        return tasks;
    }

    /**
     * Create a {@link List} of content verification {@link Callable}s. For each content URI in the collection create a
     * task that will execute against the publishing API host provided.
     *
     * @param collectionId    the Id of the collection to verify.
     * @param reader          a {@link CollectionReader} to decrypt and read the collection content.
     * @param host            the Publishing API host to use when verifying the content.
     * @param transactionId   the publishing transaction ID for this instance of the publishing API
     * @param transactionURIs the content uris to verify.
     * @return {@link List<Callable<Boolean>>>}
     */
    private List<Callable<Boolean>> createVerifyTasksForHost(String collectionId, CollectionReader reader,
                                                             String host, String transactionId,
                                                             List<String> transactionURIs) {
        return transactionURIs.stream()
                .map(uri -> new HashVerificationTask.Builder()
                        .collectionID(collectionId)
                        .collectionReader(reader)
                        .contentURI(uri)
                        .publishingAPIHost(host)
                        .transactionId(transactionId)
                        .publishingClient(publishingClient)
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> getCollectionUrisToVerify(Collection collection) throws HashVerificationException {
        try {
            return collection.getReviewed().uris()
                    .stream()
                    .filter(publishedContentFilter())
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new HashVerificationException("error getting collection reviewed uris", ex, collection.getId());
        }
    }

    /**
     * Filter the URIS in a collection that should have their SHA-1 has verified after the collection content has
     * been sent to the publishing API.
     * <ul>
     *     <li>Versioned files are skipped as these are never sent to the publishing API.</li>
     *     <li>Time series files are sent to the publising API in a zip - as a peformance imporvement we check the
     *     hash of the received zip instead of checking each file individually</li>
     * </ul>
     *
     * @return a {@link Predicate} that filters the URIs of a collection to verify.
     */
    private Predicate<String> publishedContentFilter() {
        return (uri) -> !Paths.get(uri).toString().contains("/timeseries/")
                && !VersionedContentItem.isVersionedUri(uri);
    }

    private List<Future<Boolean>> executeVerifyTasks(List<Callable<Boolean>> tasks) {
        try {
            return pool.invokeAll(tasks);
        } catch (InterruptedException ex) {
            throw new HashVerificationException("error executing content verification tasks", ex);
        }
    }

    private void checkVerifyResults(List<Future<Boolean>> verifyResults) throws HashVerificationException {
        try {
            for (Future<Boolean> result : verifyResults) {
                result.get();
            }
        } catch (Exception ex) {
            handleCheckVerifyResultsException(ex);
        }
    }

    private void handleCheckVerifyResultsException(Exception ex) throws HashVerificationException {
        if (ex instanceof ExecutionException) {
            if (ex.getCause() != null && ex.getCause() instanceof HashVerificationException) {
                throw (HashVerificationException) ex.getCause();
            }
        }
        throw new HashVerificationException("error checking verify results", ex);
    }


    /**
     * @return a singleton {@link HashVerifier} instance.
     */
    public static HashVerifier getInstance() {
        if (instance == null) {
            synchronized (HashVerifierImpl.class) {
                if (instance == null) {
                    instance = new HashVerifierImpl();
                }
            }
        }
        return instance;
    }
}
