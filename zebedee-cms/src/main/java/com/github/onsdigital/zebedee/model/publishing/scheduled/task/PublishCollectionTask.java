package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.Log;

import java.util.concurrent.Callable;

/**
 * A wrapper around the publish process of a single collection, allowing it to be executed on its own thread.
 */
public class PublishCollectionTask implements Callable<Boolean> {

    protected boolean published;
    private Collection collection;
    private ZebedeeCollectionReader collectionReader;

    /**
     * Create a new task for a collection to be published.
     * @param collection - The collection to publish.
     * @param collectionReader - The collection reader to read collection content.
     */
    public PublishCollectionTask(Collection collection, ZebedeeCollectionReader collectionReader) {
        this.collection = collection;
        this.collectionReader = collectionReader;
    }

    /**
     * Publish the collection.
     * @return
     * @throws Exception
     */
    @Override
    public Boolean call() throws Exception {
        Log.print("PUBLISH: Running publish task for collection: " + collection.description.name);
        published = Publisher.Publish(collection, "System", collectionReader);
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
