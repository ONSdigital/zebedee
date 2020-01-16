package com.github.onsdigital.zebedee.model.publishing.verify;

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

public class HashVerifierImpl implements HashVerifier {

    // TODO how big?
    private final ExecutorService pool = Executors.newFixedThreadPool(20);
    private PublishingClient publishingClient;

    public HashVerifierImpl() {
        this.publishingClient = new PublishingClientImpl();
    }

    public HashVerifierImpl(PublishingClient publishingClient) {
        this.publishingClient = publishingClient;
    }

    public void verifyTransactionContent(Collection collection, CollectionReader reader) throws IOException,
            InterruptedException, ExecutionException {

        validateParams(collection, reader);

        List<Callable<Boolean>> tasks = createVerifyTasks(collection, reader);
        List<Future<Boolean>> verifyResults = executeVerifyTasks(tasks);
        checkVerifyResults(verifyResults);
    }

    private void validateParams(Collection collection, CollectionReader reader) {
        requireNotNull(collection, "collection required but was null");
        requireNotNull(collection.getDescription(), "collection.description required but was null");
        requireNotNull(collection.getDescription().getPublishTransactionIds(), "description.publishingTransactionIds required but was null");
        requireNotNull(reader, "collection reader required but was null");
    }

    private <T> void requireNotNull(T t, String message) {
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private List<Callable<Boolean>> createVerifyTasks(Collection collection, CollectionReader reader)
            throws IOException {
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
                .map(uri -> new ContentHashVerificationTask.Builder()
                        .collectionID(collectionId)
                        .collectionReader(reader)
                        .contentURI(uri)
                        .publishingAPIHost(host)
                        .transactionId(transactionId)
                        .publishingClient(publishingClient)
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> getCollectionUrisToVerify(Collection collection) throws IOException {
        return collection.getReviewed().uris()
                .stream()
                .filter(publishedContentFilter())
                .collect(Collectors.toList());
    }

    private List<Future<Boolean>> executeVerifyTasks(List<Callable<Boolean>> tasks) throws
            InterruptedException {
        return pool.invokeAll(tasks);
    }

    private static void checkVerifyResults(List<Future<Boolean>> verifyResults) throws InterruptedException,
            ExecutionException {
        for (Future<Boolean> result : verifyResults) {
            result.get();
        }
    }

    private Predicate<String> publishedContentFilter() {
        return (uri) -> Paths.get(uri).toFile().getName().endsWith(".zip") && VersionedContentItem.isVersionedUri(uri);
    }
}
