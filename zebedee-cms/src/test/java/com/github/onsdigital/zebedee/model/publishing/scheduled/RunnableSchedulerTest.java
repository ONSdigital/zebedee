package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.model.publishing.scheduled.task.DummyExceptionTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.DummyTask;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;

public class RunnableSchedulerTest {

    RunnableScheduler runnableScheduler;

    @Before
    public void setUp() throws Exception {
        runnableScheduler = new RunnableScheduler();
    }

    @After
    public void tearDown() throws Exception {
        runnableScheduler.shutdown();
    }

    @Test
    public void schedulerShouldRunTheTask() throws InterruptedException, ExecutionException {

        // Given a scheduled task in the future.
        DummyTask task = new DummyTask();
        runnableScheduler.schedule(task, DateTime.now().plusMillis(50).toDate()).get();

        // Then the task has not run.
        assertTrue(task.hasRun);
    }

    @Test
    public void schedulerShouldRunTheTaskIfItsDateIsInThePast() throws ExecutionException, InterruptedException {

        // Given a scheduled task in the past.
        DummyTask task = new DummyTask();
        runnableScheduler.schedule(task, DateTime.now().minusMillis(100).toDate()).get();

        // Then the task has not run.
        assertTrue(task.hasRun);
    }

    @Test
    public void schedulerShouldKeepRunningTasksIfOneFails() throws InterruptedException, ExecutionException {

        // Given a scheduled task that fails with an exception.
        DummyExceptionTask exceptionTask = new DummyExceptionTask();
        runnableScheduler.schedule(exceptionTask, DateTime.now().plusMillis(50).toDate());

        DummyTask task = new DummyTask();
        ScheduledFuture<?> future = runnableScheduler.schedule(task, DateTime.now().plusMillis(100).toDate());

        future.get();

        // Then check that both tasks have run.
        assertTrue(exceptionTask.hasRun);
        assertTrue(task.hasRun);
    }

    @Test
    public void schedulerShouldScheduleWithTheCorrectTime() throws InterruptedException {

        // Given a scheduled task that fails with an exception.
        DummyTask task = new DummyTask();
        ScheduledFuture<?> future = runnableScheduler.schedule(task, DateTime.now().plusDays(1).toDate());

        long delayInSeconds = future.getDelay(TimeUnit.SECONDS);
        assertTrue(delayInSeconds > 86395);
    }

    @Test
    public void scheduleShouldRunMultipleTasks() throws InterruptedException, ExecutionException {

        // Given a scheduled task in the future.
        List<DummyTask> tasks = new ArrayList<>();
        List<ScheduledFuture> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            System.out.println("Scheduling task " + i);
            DummyTask task = new DummyTask();
            tasks.add(task);
            ScheduledFuture<?> future = runnableScheduler.schedule(task, new Date(System.currentTimeMillis() + 200));
            futures.add(future);
        }

        for (ScheduledFuture future : futures) {
            future.get();
        }

        // Then the task has not run.
        int j = 0;
        for (DummyTask task : tasks) {
            System.out.println("Task " + j + " finished = " + task.hasRun);
            assertTrue(task.hasRun);
            ++j;
        }
    }

    @Test
    public void scheduleShouldTakeMillisecondsIntoAccount() throws InterruptedException, ExecutionException {

        System.out.println("scheduleShouldTakeMillisecondsIntoAccount start");
        // Given a scheduled task that fails with an exception.
        DummyTask task = new DummyTask();
        Date now = new Date(System.currentTimeMillis() + 1333);
        ScheduledFuture<?> future = runnableScheduler.schedule(task, now);

        if (future == null) {
            System.out.println("scheduleShouldTakeMillisecondsIntoAccount - future is null");
        }

        // When the time for the task passes.
        long delayInMs = future.getDelay(TimeUnit.MILLISECONDS);
        assertTrue(delayInMs > 1300);

        System.out.println("scheduleShouldTakeMillisecondsIntoAccount end");
    }
}


