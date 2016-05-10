package com.github.onsdigital.zebedee.model.publishing.scheduled;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Schedule a task to run at the given date.
 */
public class RunnableScheduler {

    ScheduledExecutorService scheduledExecutorService;

    public RunnableScheduler() {
        this(10); // default thread pool size of 10;
    }

    public RunnableScheduler(int poolSize) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(poolSize);
        executor.setRemoveOnCancelPolicy(true);
        this.scheduledExecutorService = executor;
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
        logInfo("Pending scheduled task info").addParameter("MS_leftTillStart", +ms).log();
        return future;
    }

    /**
     * Shutdown the scheduler to ensure resources are disposed of.
     */
    public void shutdown() {
        logDebug("Shutdown called on Scheduler.").log();
        scheduledExecutorService.shutdown();
    }
}
