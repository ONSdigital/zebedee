package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * A scheduler that maintains tasks based on collections.
 */
public class CollectionScheduler {

    private final Scheduler scheduler = new Scheduler();
    private final Map<String, ScheduledFuture<?>> collectionIdToTask = new ConcurrentHashMap<>();

    public static void schedulePublish(CollectionScheduler scheduler, Collection collection, Zebedee zebedee) {
        if (Configuration.isSchedulingEnabled()) {
            try {
                System.out.println("Attempting to schedule publish for collection " + collection.description.name + " type=" + collection.description.type);
                if (collection.description.type == CollectionType.scheduled) {
                    scheduler.schedule(collection, new PublishTask(zebedee, collection));
                }
            } catch (Exception e) {
                System.out.println("Exception caught trying to schedule existing collection: " + e.getMessage());
            }
        } else {
            Log.print("Not scheduling collection %s, scheduling is not enabled", collection.description.name);
        }
    }

    /**
     * Schedule a task related to a collection.
     *
     * @param collection
     * @param task
     */
    public boolean schedule(Collection collection, Runnable task) {

        // throw exception if a manual collection is given.
        if (collection.description.type == CollectionType.manual) {
            Log.print("Not scheduling this collection as it is manual collection.");
            return false;
        }

        if (taskExistsForCollection(collection)) {
            Log.print("A task already exists for collection" + collection.description.id + " this will now be cancelled");
            cancel(collection);
        }

        Log.print("Scheduling task for collection: " + collection.description.id);
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
     * Return the task associated with this collection.
     *
     * @param collection
     * @return
     */
    public ScheduledFuture<?> getTaskForCollection(Collection collection) {
        return collectionIdToTask.get(collection.description.id);
    }

    /**
     * Cancel and remove the task for the given collection.
     *
     * @param collection
     */
    public void cancel(Collection collection) {

        System.out.println("Attempting to cancel task for collection " + collection.description.name);
        ScheduledFuture<?> future = collectionIdToTask.get(collection.description.id);

        if (future != null) {
            boolean response = future.cancel(false);
            collectionIdToTask.remove(collection.description.id);
            System.out.println("Task cancelled for collection " + collection.description.name + " response=" + response);
        }
    }

    /**
     * Shutdown the scheduler to ensure resources are disposed of.
     */
    public void shutdown() {
        System.out.println("Shutdown called on CollectionScheduler.");
        scheduler.shutdown();
    }
}
