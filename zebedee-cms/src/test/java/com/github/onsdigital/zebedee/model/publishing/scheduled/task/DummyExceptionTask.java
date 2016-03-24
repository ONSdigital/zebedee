package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

/**
 * Runnable that throws an exception to test error handling.
 */
public class DummyExceptionTask extends DummyTask {
    @Override
    public void run() {
        System.out.println("DummyExceptionTask has run at " + System.currentTimeMillis());
        hasRun = true;
        throw new RuntimeException();
    }
}
