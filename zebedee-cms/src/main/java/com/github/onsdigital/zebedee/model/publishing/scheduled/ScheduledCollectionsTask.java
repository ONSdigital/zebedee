package com.github.onsdigital.zebedee.model.publishing.scheduled;


import com.github.onsdigital.zebedee.model.Collection;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * Abstract class representing a task that runs at a particular time for a number of collections.
 */
public abstract class ScheduledCollectionsTask implements Runnable {

    private final Set<String> collectionIds; // The list of collections ID's used in the task.
    private final ScheduledFuture<?> future; // The reference to the future of the task.

    Scheduler scheduler = new Scheduler(1);

    public ScheduledCollectionsTask(Date scheduledDate) {
        this.collectionIds = new HashSet<>();
        future = scheduler.schedule(this, scheduledDate);
    }

    public void addCollection(Collection collection) {
        collectionIds.add(collection.description.id);
    }

    public void removeCollection(Collection collection) {
        collectionIds.remove(collection.description.id);
    }

    public void cancel() {
        future.cancel(false);
    }
}
