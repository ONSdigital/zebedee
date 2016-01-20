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

    protected Set<String> collectionIds; // The list of collections ID's used in the task.
    protected ScheduledFuture<?> future; // The reference to the future of the task.
    protected Date scheduledDate;

    Scheduler scheduler = new Scheduler(1);

    public ScheduledCollectionsTask() {
        this.collectionIds = new HashSet<>();
    }

    /**
     * Returns false if the task is already scheduled.
     * @param scheduledDate
     * @return
     */
    public boolean schedule(Date scheduledDate) {
        if (this.future != null && this.scheduledDate != null) {
            return false;
        }

        this.scheduledDate = scheduledDate;
        future = scheduler.schedule(this, scheduledDate);
        return true;
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

    public boolean isComplete() {
        return future != null && future.isDone();
    }
}
