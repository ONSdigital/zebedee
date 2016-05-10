package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Dummy task to test if it has been run.
 */
public class DummyTask implements Runnable {

    public final String id = Random.id();
    public boolean hasRun = false;

    public DummyTask() {
        logDebug("Created dummy task").addParameter("taskId", id).log();
    }

    @Override
    public void run() {
        logDebug("Dummy task completed").timeTaken(System.currentTimeMillis()).addParameter("taskId", id).log();
        hasRun = true;
    }
}
