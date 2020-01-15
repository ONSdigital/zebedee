package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.SlackNotification;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;

/**
 * A wrapper around the publish process of a single collection, allowing it to be executed on its own thread.
 */
public class PublishCollectionTask implements Callable<Boolean> {

    private static String publisherSystemEmail = "System";
    protected boolean published;
    private Collection collection;
    private ZebedeeCollectionReader collectionReader;
    private Map<String, String> hostToTransactionIdMap;

    /**
     * Create a new task for a collection to be published.
     *
     * @param collection       - The collection to publish.
     * @param collectionReader - The collection reader to read collection content.
     */
    public PublishCollectionTask(Collection collection, ZebedeeCollectionReader collectionReader, Map<String, String> hostToTransactionIdMap) {
        this.collection = collection;
        this.collectionReader = collectionReader;
        this.hostToTransactionIdMap = hostToTransactionIdMap;
    }

    /**
     * Publish the collection.
     *
     * @return
     * @throws Exception
     */
    @Override
    public Boolean call() throws Exception {

        String collectionId = collection.getDescription().getId();

        try {
            info().data("collectionId", collectionId).log("PUBLISH: Running collection publish task");
            collection.getDescription().publishStartDate = new Date();

            published = Publisher.executePublish(collection, collectionReader, publisherSystemEmail);

            collection.getDescription().publishEndDate = new Date();
        } catch (Exception e) {
            // If an error was caught, attempt to roll back the transaction:
            Map<String, String> transactionIdMap = collection.getDescription().getPublishTransactionIds();

            if (transactionIdMap != null && !transactionIdMap.isEmpty()) {
                error().data("collectionId", collectionId).data("hostToTransactionId", transactionIdMap)
                        .logException(e, "PUBLISH: FAILURE: exception while attempting to publish scheduled collection. " +
                                "Publishing transaction IDS exist for collection, attempting to rollback");

                Publisher.rollbackPublish(collection);

            } else {
                error().data("collectionId", collectionId).data("hostToTransactionId", transactionIdMap)
                        .logException(e, "PUBLISH: FAILURE: no publishing transaction IDS found for collection, no rollback attempt will be made");
            }
        } finally {
            Map<String, String> transactionIdMap = collection.getDescription().getPublishTransactionIds();
            try {
                // Save any updates to the collection
                info().data("collectionId", collectionId).data("hostToTransactionId", transactionIdMap)
                        .log("PUBLISH: persiting changes to collection to disk");
                collection.save();
            } catch (Exception e) {
                error().data("collectionId", collectionId).data("hostToTransactionId", transactionIdMap)
                        .logException(e, "PUBLISH: error while attempting to persist collection to disk");
                throw e;
            }
            if (!published) {
                warn().data("collectionId", collectionId).log("Exception publishing scheduled collection");

                SlackNotification.scheduledPublishFailure(collection);
            }
            return published;
        }
    }

    /**
     * Return true if the publish was a success.
     *
     * @return
     */
    public boolean isPublished() {
        return published;
    }

    /**
     * Get the collection associated with this task.
     *
     * @return
     */
    public Collection getCollection() {
        return collection;
    }

    /**
     * Get the collection reader associated with this task.
     *
     * @return
     */
    public ZebedeeCollectionReader getCollectionReader() {
        return collectionReader;
    }
}
