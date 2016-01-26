package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.util.Log;

import java.util.concurrent.ScheduledFuture;

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
        Log.print("Running dummy task with ID %s", id);
        runComplete = true;
    }

    public boolean isRunComplete() {
        return runComplete;
    }

    public ScheduledFuture<?> getFuture() {
        return this.future;
    }
}
