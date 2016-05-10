package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;

import java.util.concurrent.ScheduledFuture;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * ScheduledTask implementation that does nothing but track if it has been run.
 */
public class DummyScheduledTask extends ScheduledTask {

    private boolean runComplete = false;
    final String id = Random.id();

    public DummyScheduledTask() {
        super();
    }

    @Override
    public void run() {
        logDebug("Running dummy task").addParameter("taskId", id).log();
        runComplete = true;
    }

    public boolean isRunComplete() {
        return runComplete;
    }

    public ScheduledFuture<?> getFuture() {
        return this.future;
    }
}
