package com.github.onsdigital.zebedee.model.publishing.scheduled.task;


import com.github.onsdigital.zebedee.model.publishing.scheduled.RunnableScheduler;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

/**
 * Abstract class representing a task that runs at a scheduled time.
 * This class takes care of task execution allowing the extended class to just implement
 * the actual job of the task.
 */
public abstract class ScheduledTask implements Runnable {

    protected ScheduledFuture<?> future; // The reference to the future of the task.
    protected Date scheduledDate;

    RunnableScheduler runnableScheduler = new RunnableScheduler(1);

    /**
     * Set the task to execute at the give date.
     *
     * @param scheduledDate
     * @return
     */
    public boolean schedule(Date scheduledDate) {
        if (this.future != null && this.scheduledDate != null) {
            return false;
        }

        this.scheduledDate = scheduledDate;
        future = runnableScheduler.schedule(this, scheduledDate);
        return true;
    }

    /**
     * Cancel this task.
     */
    public void cancel() {
        future.cancel(false);
    }

    /**
     * Returns true if the task has been run / completed.
     * @return
     */
    public boolean isComplete() {
        return future != null && future.isDone();
    }
}
