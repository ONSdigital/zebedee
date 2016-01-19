package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.util.Log;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Schedule a task to run at the given date.
 */
public class Scheduler {

    ScheduledThreadPoolExecutor scheduledExecutorService;

    public Scheduler() {
        scheduledExecutorService = new ScheduledThreadPoolExecutor(10);
        scheduledExecutorService.setRemoveOnCancelPolicy(true);
    }

    /**
     * Schedule a task to run on the specified date.
     *
     * @param task
     * @param scheduledDate
     */
    public ScheduledFuture<?> schedule(Runnable task, Date scheduledDate) {
        long nowMs = System.currentTimeMillis();
        long ms = scheduledDate.getTime() - nowMs;
        ScheduledFuture<?> future = scheduledExecutorService.schedule(task, ms, TimeUnit.MILLISECONDS);
        Log.print("Task scheduled to run in " + ms + "ms.");
        return future;
    }

    /**
     * Shutdown the scheduler to ensure resources are disposed of.
     */
    public void shutdown() {
        System.out.println("Shutdown called on Scheduler.");
        scheduledExecutorService.shutdown();
    }
}
