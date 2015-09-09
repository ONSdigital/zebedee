package com.github.onsdigital.zebedee.model.publishing;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class SchedulerTest {

    Scheduler scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = new Scheduler();
    }

    @After
    public void tearDown() throws Exception {
        scheduler.shutdown();
    }

    @Test
    public void shutdownShouldPreventFurtherTasksBeingExecuted() {

        // Given a scheduled task in the future.
        DummyTask task = new DummyTask();
        scheduler.schedule(task, DateTime.now().plusSeconds(5).toDate());

        // Then the task has not run.
        assertFalse(task.hasRun);
    }

    @Test
    public void schedulerShouldRunTheTask() throws InterruptedException {

        // Given a scheduled task in the future.
        DummyTask task = new DummyTask();
        scheduler.schedule(task, DateTime.now().plusMillis(50).toDate());

        // When the time for the task passes.
        Thread.sleep(200);

        // Then the task has not run.
        assertTrue(task.hasRun);
    }

    @Test
    public void schedulerShouldRunTheTaskIfItsDateIsInThePast() throws InterruptedException {

        // Given a scheduled task in the past.
        DummyTask task = new DummyTask();
        scheduler.schedule(task, DateTime.now().minusMillis(100).toDate());

        // When the time for the task passes.
        Thread.sleep(50);

        // Then the task has not run.
        assertTrue(task.hasRun);
    }

    @Test
    public void schedulerShouldKeepRunningTasksIfOneFails() throws InterruptedException {

        // Given a scheduled task that fails with an exception.

        DummyExceptionTask exceptionTask = new DummyExceptionTask();
        scheduler.schedule(exceptionTask, DateTime.now().plusMillis(50).toDate());

        DummyTask task = new DummyTask();
        scheduler.schedule(task, DateTime.now().plusMillis(100).toDate());

        // When the time for the task passes.
        Thread.sleep(300);

        // Then check that both tasks have run.
        assertTrue(exceptionTask.hasRun);
        assertTrue(task.hasRun);
    }

    @Test
    public void schedulerShouldScheduleWithTheCorrectTime() throws InterruptedException {

        // Given a scheduled task that fails with an exception.
        DummyTask task = new DummyTask();
        ScheduledFuture<?> future = scheduler.schedule(task, DateTime.now().plusDays(1).toDate());

        long delayInSeconds = future.getDelay(TimeUnit.SECONDS);

        assertTrue(delayInSeconds > 86395);
    }

    @Test
    public void scheduleShouldRunMultipleTasks() throws InterruptedException {

        // Given a scheduled task in the future.
        List<DummyTask> tasks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            System.out.println("Scheduling task " + i);
            DummyTask task = new DummyTask();
            tasks.add(task);
            scheduler.schedule(task, new Date(System.currentTimeMillis() + 200));
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
    }

    @Test
    public void scheduleShouldTakeMillisecondsIntoAccount() throws InterruptedException {

        // Given a scheduled task that fails with an exception.
        DummyTask task = new DummyTask();
        Date now = new Date(System.currentTimeMillis() + 1333);
        ScheduledFuture<?> future = scheduler.schedule(task, now);

        // When the time for the task passes.
        long delayInMs = future.getDelay(TimeUnit.MILLISECONDS);
        assertTrue(delayInMs > 1300);
    }
}


