package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.util.Log;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScheduledCollectionTaskTest {

    @Test
    public void testScheduleRunsTask() throws InterruptedException {

        // Given a new task.
        DummyScheduledCollectionsTask task = new DummyScheduledCollectionsTask();
        Date scheduledDate = new Date(); // now

        // When the task is scheduled with a scheduled date of now.
        assertTrue(task.schedule(scheduledDate));


        // Then the task should run immediately
        if (!task.getFuture().isDone()) {
            // if the task is not already done, wait a bit.
            Log.print("Waiting for the task!");
            Thread.sleep(5);
        }
        assertTrue(task.getFuture().isDone());
        assertTrue(task.isRunComplete());
    }

    @Test
    public void testScheduleReturnsFalseIfAlreadyScheduled() throws InterruptedException {

        // Given a task that is already scheduled.
        DummyScheduledCollectionsTask task = new DummyScheduledCollectionsTask();
        Date scheduledDate = DateTime.now().plusSeconds(5).toDate();
        assertTrue(task.schedule(scheduledDate));

        // When schedule is called a second time
        boolean scheduled = task.schedule(scheduledDate);

        // Then the return value is false
        assertFalse(scheduled);

        task.cancel(); // cancel the pending task.
    }

    @Test
    public void testCancel() throws InterruptedException {

        // Given a scheduled task
        DummyScheduledCollectionsTask task = new DummyScheduledCollectionsTask();
        Date scheduledDate = DateTime.now().plusSeconds(5).toDate();
        task.schedule(scheduledDate);

        // When cancel is called.
        task.cancel();

        // The task is cancelled and not run.
        assertFalse(task.isRunComplete());
        assertTrue(task.getFuture().isCancelled());
    }
}
