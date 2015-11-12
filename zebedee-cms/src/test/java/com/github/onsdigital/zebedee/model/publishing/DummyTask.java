package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.util.Log;

/**
 * Dummy task to test if it has been run.
 */
public class DummyTask implements Runnable {

    final String id = Random.id();
    boolean hasRun = false;

    public DummyTask() {
        Log.print("Created dummy task with ID %s", id);
    }

    @Override
    public void run() {
        Log.print("DummyTask %s has run at " + System.currentTimeMillis(), id);
        hasRun = true;
    }
}
