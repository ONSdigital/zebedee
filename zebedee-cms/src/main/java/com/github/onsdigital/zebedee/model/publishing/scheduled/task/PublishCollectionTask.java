package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.SlackNotification;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

/**
 * A wrapper around the publish process of a single collection, allowing it to be executed on its own thread.
 */
public class PublishCollectionTask implements Callable<Boolean> {

    private static String publisherSystemEmail = "System";
    protected boolean published;
    private Collection collection;
    private ZebedeeCollectionReader collectionReader;
    private String encryptionPassword;
    private Map<String, String> hostToTransactionIdMap;

    /**
     * Create a new task for a collection to be published.
     *
     * @param collection         - The collection to publish.
     * @param collectionReader   - The collection reader to read collection content.
     * @param encryptionPassword
     */
    public PublishCollectionTask(Collection collection, ZebedeeCollectionReader collectionReader, String encryptionPassword, Map<String, String> hostToTransactionIdMap) {
        this.collection = collection;
        this.collectionReader = collectionReader;
        this.encryptionPassword = encryptionPassword;
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
        try {
            logInfo("PUBLISH: Running collection publish task").collectionId(collection).log();
            collection.getDescription().publishStartDate = new Date();

            Publisher.publishFilteredCollectionFiles(collection, collectionReader, encryptionPassword);

            published = Publisher.commitPublish(collection, publisherSystemEmail, encryptionPassword);
            collection.getDescription().publishEndDate = new Date();
        } catch (Exception e) {
            // If an error was caught, attempt to roll back the transaction:
            if (collection.getDescription().publishTransactionIds != null &&
                    !collection.getDescription().publishTransactionIds.isEmpty()) {
                logError(e, "PUBLISH: FAILURE: exception while attempting to publish scheduled collection. " +
                        "Publishing transaction IDS exist for collection, attempting to rollback")
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();

                Publisher.rollbackPublish(collection, encryptionPassword);

            } else {
                logError(e, "PUBLISH: FAILURE: no publishing transaction IDS found for collection, no rollback " +
                        "attempt will be made")
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();
            }
        } finally {
            try {
                // Save any updates to the collection
                logInfo("PUBLISH: persiting changes to collection to disk")
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();
                collection.save();
            } catch (Exception e) {
                logError(e, "PUBLISH: error while attempting to persist collection to disk")
                        .collectionId(collection)
                        .hostToTransactionID(collection.getDescription().publishTransactionIds)
                        .log();
                throw e;
            }
            if (!published) {
                logWarn("Exception publishing scheduled collection")
                        .collectionId(collection)
                        .collectionName(collection)
                        .log();

                SlackNotification.scheduledPublishFailire(collection);
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
