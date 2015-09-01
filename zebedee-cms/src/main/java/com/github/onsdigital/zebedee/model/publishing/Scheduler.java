package com.github.onsdigital.zebedee.model.publishing;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.joda.time.Seconds.secondsBetween;

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
     * @param date
     */
    public ScheduledFuture<?> schedule(Runnable task, Date date) {
        DateTime now = DateTime.now();
        DateTime scheduledDate = new DateTime(date);
        int seconds = secondsBetween(now, scheduledDate).getSeconds();

        System.out.println("Task scheduled to run in " + seconds + " seconds. Current time = " + now.toString() + " Schedule time=" + scheduledDate.toString());
        ScheduledFuture<?> future = scheduledExecutorService.schedule(task, seconds, TimeUnit.SECONDS);
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
