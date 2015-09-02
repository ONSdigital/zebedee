package com.github.onsdigital.zebedee.model.publishing;

/**
 * Dummy task to test if it has been run.
 */
public class DummyTask implements Runnable {

    boolean hasRun = false;

    @Override
    public void run() {
        System.out.println("DummyTask has run at " + System.currentTimeMillis());
        hasRun = true;
    }
}
