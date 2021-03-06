package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;

import java.util.Date;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

public class DummyPostPublishCollectionTask extends PostPublishCollectionTask {

    final String id = Random.id();
    private final int duration;
    private Date start;
    private Date end;


    public DummyPostPublishCollectionTask(int durationMillis) {
        super(null, null);
        this.duration = durationMillis;
    }

    /**
     * Dummy publish task which does nothing other than set that the publish is complete.
     */
    public DummyPostPublishCollectionTask() {
        super(null, null);
        this.duration = 0;
    }

    public DummyPostPublishCollectionTask(DummyPublishCollectionTask publish1) {
        super(null, publish1);
        this.duration = 0;
    }


    @Override
    public Boolean call() throws Exception {
        this.start = new Date();

        info().data("taskId", id).log("Running dummy post-publish task");

        Thread.sleep(duration);
        this.done = true;

        info().data("taskId", id).log("Finished dummy post-publish task");
        this.end = new Date();
        return true;
    }

    public String getId() {
        return id;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }
}
