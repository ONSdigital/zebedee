package com.github.onsdigital.zebedee.model.publishing.scheduled;


import java.util.Date;
import java.util.concurrent.ScheduledFuture;

/**
 * Abstract class representing a task that runs at a particular time for a number of collections.
 */
public abstract class ScheduledTask implements Runnable {

    protected ScheduledFuture<?> future; // The reference to the future of the task.
    protected Date scheduledDate;

    Scheduler scheduler = new Scheduler(1);

    /**
     * Returns false if the task is already scheduled.
     *
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

    public void cancel() {
        future.cancel(false);
    }

    public boolean isComplete() {
        return future != null && future.isDone();
    }
}
