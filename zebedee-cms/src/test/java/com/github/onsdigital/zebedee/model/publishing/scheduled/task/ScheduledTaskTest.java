package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import org.joda.time.DateTime;

import java.util.Date;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScheduledTaskTest {

    //@Test
    public void testScheduleRunsTask() throws InterruptedException {

        // Given a new task.
        DummyScheduledTask task = new DummyScheduledTask();
        Date scheduledDate = new Date(); // now

        // When the task is scheduled with a scheduled date of now.
        assertTrue(task.schedule(scheduledDate));


        // Then the task should run immediately
        if (!task.getFuture().isDone()) {
            // if the task is not already done, wait a bit.
            logDebug("Waiting for the task!").log();
            Thread.sleep(5);
        }
        assertTrue(task.getFuture().isDone());
        assertTrue(task.isRunComplete());
    }

    //@Test
    public void testScheduleReturnsFalseIfAlreadyScheduled() throws InterruptedException {

        // Given a task that is already scheduled.
        DummyScheduledTask task = new DummyScheduledTask();
        Date scheduledDate = DateTime.now().plusSeconds(5).toDate();
        assertTrue(task.schedule(scheduledDate));

        // When schedule is called a second time
        boolean scheduled = task.schedule(scheduledDate);

        // Then the return value is false
        assertFalse(scheduled);

        task.cancel(); // cancel the pending task.
    }

    //@Test
    public void testCancel() throws InterruptedException {

        // Given a scheduled task
        DummyScheduledTask task = new DummyScheduledTask();
        Date scheduledDate = DateTime.now().plusSeconds(5).toDate();
        task.schedule(scheduledDate);

        // When cancel is called.
        task.cancel();

        // The task is cancelled and not run.
        assertFalse(task.isRunComplete());
        assertTrue(task.getFuture().isCancelled());
    }
}
