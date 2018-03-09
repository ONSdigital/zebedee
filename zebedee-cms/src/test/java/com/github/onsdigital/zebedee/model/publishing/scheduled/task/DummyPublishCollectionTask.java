package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.model.Collection;

import java.util.Date;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

public class DummyPublishCollectionTask extends PublishCollectionTask {

    final String id = Random.id();

    private final int duration;
    private Date start;
    private Date end;

    public DummyPublishCollectionTask(Collection collection, int durationMillis) {
        super(collection, null, "", null);
        this.duration = durationMillis;
    }

    @Override
    public Boolean call() throws Exception {

        this.start = new Date();
        logDebug("Running dummy publish task").addParameter("taskId", id).log();

        Thread.sleep(duration);
        this.published = true;

        logDebug("Finished dummy publish task").addParameter("taskId", id).log();
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
