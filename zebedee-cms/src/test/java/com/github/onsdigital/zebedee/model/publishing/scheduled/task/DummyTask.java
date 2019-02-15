package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Dummy task to test if it has been run.
 */
public class DummyTask implements Runnable {

    public final String id = Random.id();
    public boolean hasRun = false;

    public DummyTask() {
        info().data("taskId", id).log("Created dummy task");
    }

    @Override
    public void run() {
        info().data("timeTaken", System.currentTimeMillis()).data("taskId", id)
                .log("Dummy task completed");
        hasRun = true;
    }
}
