package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.util.Log;

/**
 * Dummy task to test if it has been run.
 */
public class DummyTask implements Runnable {

    boolean hasRun = false;

    @Override
    public void run() {
        Log.print("DummyTask has run at " + System.currentTimeMillis());
        hasRun = true;
    }
}
