package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;

import java.util.concurrent.ScheduledFuture;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

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
        info().data("taskId", id).log("Running dummy task");
        runComplete = true;
    }

    public boolean isRunComplete() {
        return runComplete;
    }

    public ScheduledFuture<?> getFuture() {
        return this.future;
    }
}
