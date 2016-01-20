package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.util.Log;

import java.util.concurrent.ScheduledFuture;

public class DummyScheduledCollectionsTask extends ScheduledCollectionsTask {

    private boolean runComplete = false;
    final String id = Random.id();

    public DummyScheduledCollectionsTask() {
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
