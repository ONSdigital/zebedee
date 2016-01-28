package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.util.Log;

import java.util.Date;

public class DummyPublishCollectionTask extends PublishCollectionTask {

    final String id = Random.id();

    private final int duration;
    private Date start;
    private Date end;

    /**
     * Dummy publish task which does nothing other than set that the publish is complete.
     */
    public DummyPublishCollectionTask() {
        super(null, null, "", null);
        duration = 0;
    }

    public DummyPublishCollectionTask(int durationMillis) {
        super(null, null, "", null);
        this.duration = durationMillis;
    }

    @Override
    public Boolean call() throws Exception {

        this.start = new Date();
        Log.print("Running dummy publish task with ID %s", id);

        Thread.sleep(duration);
        this.published = true;

        Log.print("Finished dummy publish task with ID %s", id);
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
