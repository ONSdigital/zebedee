package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.Log;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.debugMessage;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

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
     * @return
     * @throws Exception
     */
    @Override
    public Boolean call() throws Exception {
        Log.print("PUBLISH: Running publish task for collection: " + collection.description.name);

        try {
            collection.description.publishStartDate = new Date();

            Publisher.PublishFilteredCollectionFiles(collection, collectionReader, encryptionPassword);

            published = Publisher.CommitPublish(collection, publisherSystemEmail, encryptionPassword);
            collection.description.publishEndDate = new Date();
        } catch (IOException e) {
            logError(e).errorContext("Exception publishing collection").collectionName(collection).log();
            // If an error was caught, attempt to roll back the transaction:
            if (collection.description.publishTransactionIds != null) {
                debugMessage("Attempting rollback of publishing transaction").collectionName(collection).log();
                Publisher.rollbackPublish(hostToTransactionIdMap, encryptionPassword);
            }
        } finally {
            // Save any updates to the collection
            collection.save();
        }

        return published;
    }

    /**
     * Return true if the publish was a success.
     * @return
     */
    public boolean isPublished() {
        return published;
    }

    /**
     * Get the collection associated with this task.
     * @return
     */
    public Collection getCollection() {
        return collection;
    }

    /**
     * Get the collection reader associated with this task.
     * @return
     */
    public ZebedeeCollectionReader getCollectionReader() {
        return collectionReader;
    }
}
