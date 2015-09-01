package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * A scheduler that maintains tasks based on collections.
 */
public class CollectionScheduler {

    private final Scheduler scheduler = new Scheduler();
    private final Map<String, ScheduledFuture<?>> collectionIdToTask = new ConcurrentHashMap<>();

    /**
     * Schedule a task related to a collection.
     *
     * @param collection
     * @param task
     */
    public boolean schedule(Collection collection, Runnable task) {

        // throw exception if a manual collection is given.
        if (collection.description.type == CollectionType.manual) {
            System.out.println("Not scheduling this collection as it is manual collection.");
            return false;
        }

        System.out.println("Scheduling task for collection: " + collection.description.id);
        ScheduledFuture<?> future = scheduler.schedule(task, collection.description.publishDate);
        collectionIdToTask.put(collection.description.id, future);
        return true;
    }

    /**
     * Return true if there is a task scheduled for the given collection.
     *
     * @param collection
     * @return
     */
    public boolean taskExistsForCollection(Collection collection) {
        return collectionIdToTask.containsKey(collection.description.id);
    }

    /**
     * Cancel and remove the task for the given collection.
     *
     * @param collection
     */
    public void cancel(Collection collection) {
        boolean response = collectionIdToTask.get(collection.description.id).cancel(false);
        collectionIdToTask.remove(collection.description.id);
        System.out.println("Task cancelled for collection " + collection.description.name + " response=" + response);
    }

    /**
     * Shutdown the scheduler to ensure resources are disposed of.
     */
    public void shutdown() {
        System.out.println("Shutdown called on CollectionScheduler.");
        scheduler.shutdown();
    }
}
