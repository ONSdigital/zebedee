package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.httpino.Host;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.Log;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A wrapper around the publish process of a single collection, allowing it to be executed on its own thread.
 */
public class PublishCollectionTask implements Callable<Boolean> {

    private static String publisherSystemEmail = "System";
    protected boolean published;
    private Collection collection;
    private ZebedeeCollectionReader collectionReader;
    private String encryptionPassword;
    private Map<Host, String> hostToTransactionIdMap;

    /**
     * Create a new task for a collection to be published.
     * @param collection - The collection to publish.
     * @param collectionReader - The collection reader to read collection content.
     * @param encryptionPassword
     */
    public PublishCollectionTask(Collection collection, ZebedeeCollectionReader collectionReader, String encryptionPassword, Map<Host, String> hostToTransactionIdMap) {
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
            published = Publisher.CommitPublish(collection, publisherSystemEmail, encryptionPassword);
        } catch (IOException e) {
            Log.print("Exception publishing collection: %s: %s", collection.description.name, e.getMessage());
            System.out.println(ExceptionUtils.getStackTrace(e));
            // If an error was caught, attempt to roll back the transaction:
            if (collection.description.publishTransactionIds != null) {
                Log.print("Attempting rollback of publishing transaction for collection: " + collection.description.name);
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
