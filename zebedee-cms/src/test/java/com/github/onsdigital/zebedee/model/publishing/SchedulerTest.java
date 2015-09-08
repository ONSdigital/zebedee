package com.github.onsdigital.zebedee.model.publishing;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class SchedulerTest {

    @Test
    public void shutdownShouldNotThrowExceptions() {
        Scheduler scheduler = new Scheduler();
        scheduler.shutdown();
    }

    @Test
    public void shutdownShouldPreventFurtherTasksBeingExecuted() {

        // Given a scheduled task in the future.
        Scheduler scheduler = new Scheduler();
        DummyTask task = new DummyTask();
        scheduler.schedule(task, DateTime.now().plusSeconds(5).toDate());

        // When the shutdown method is called
        scheduler.shutdown();

        // Then the task has not run.
        assertFalse(task.hasRun);
    }

    @Test
    public void schedulerShouldRunTheTask() throws InterruptedException {

        // Given a scheduled task in the future.
        Scheduler scheduler = new Scheduler();
        DummyTask task = new DummyTask();
        scheduler.schedule(task, DateTime.now().plusMillis(100).toDate());

        // When the time for the task passes.
        Thread.sleep(200);

        // Then the task has not run.
        assertTrue(task.hasRun);

        scheduler.shutdown();
    }

    @Test
    public void schedulerShouldRunTheTaskIfItsDateIsInThePast() throws InterruptedException {

        // Given a scheduled task in the past.
        Scheduler scheduler = new Scheduler();
        DummyTask task = new DummyTask();
        scheduler.schedule(task, DateTime.now().minusMillis(100).toDate());

        // When the time for the task passes.
        Thread.sleep(50);

        // Then the task has not run.
        assertTrue(task.hasRun);

        scheduler.shutdown();
    }

    @Test
    public void schedulerShouldKeepRunningTasksIfOneFails() throws InterruptedException {

        // Given a scheduled task that fails with an exception.
        Scheduler scheduler = new Scheduler();

        DummyExceptionTask exceptionTask = new DummyExceptionTask();
        scheduler.schedule(exceptionTask, DateTime.now().plusMillis(100).toDate());

        DummyTask task = new DummyTask();
        scheduler.schedule(task, DateTime.now().plusMillis(200).toDate());

        // When the time for the task passes.
        Thread.sleep(300);

        // Then check that both tasks have run.
        assertTrue(exceptionTask.hasRun);
        assertTrue(task.hasRun);

        scheduler.shutdown();
    }

    @Test
    public void schedulerShouldScheduleWithTheCorrectTime() throws InterruptedException {

        // Given a scheduled task that fails with an exception.
        Scheduler scheduler = new Scheduler();

        DummyTask task = new DummyTask();
        ScheduledFuture<?> future = scheduler.schedule(task, DateTime.now().plusDays(1).toDate());

        long delayInSeconds = future.getDelay(TimeUnit.SECONDS);

        assertTrue(delayInSeconds > 86395);

        scheduler.shutdown();
    }

    @Test
    public void scheduleShouldRunMultipleTasks() throws InterruptedException {

        // Given a scheduled task in the future.
        Scheduler scheduler = new Scheduler();

        List<DummyTask> tasks = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            System.out.println("Scheduling task " + i);
            DummyTask task = new DummyTask();
            tasks.add(task);
            scheduler.schedule(task, DateTime.now().plusMillis(200).toDate());
        }

        // When the time for the task passes.
        Thread.sleep(300);

        // Then the task has not run.
        int j = 0;
        for (DummyTask task : tasks) {
            System.out.println("Task " + j + " finished = " + task.hasRun);
            assertTrue(task.hasRun);
            ++j;
        }

        scheduler.shutdown();
    }

    @Test
    public void scheduleShouldTakeMillisecondsIntoAccount() throws InterruptedException {

        // Given a scheduled task that fails with an exception.
        Scheduler scheduler = new Scheduler();
        DummyTask task = new DummyTask();
        Date now = new Date(System.currentTimeMillis() + 1333);
        ScheduledFuture<?> future = scheduler.schedule(task, now);

        // When the time for the task passes.
        long delayInMs = future.getDelay(TimeUnit.MILLISECONDS);
        assertTrue(delayInMs > 1300);
        scheduler.shutdown();
    }
}


