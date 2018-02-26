package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.DummyScheduledTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.ScheduledTask;

import java.util.HashMap;
import java.util.Map;

public class DummyScheduler extends Scheduler {

    private final Map<Collection, ScheduledTask> scheduledCollections;

    public DummyScheduler() {
        this.scheduledCollections = new HashMap<>();
    }

    @Override
    protected void schedule(Collection collection, Zebedee zebedee) {
        DummyScheduledTask dummyScheduledTask = new DummyScheduledTask();
        dummyScheduledTask.schedule(collection.getDescription().getPublishDate());
        scheduledCollections.put(collection, dummyScheduledTask);
    }

    @Override
    public void cancel(Collection collection) {
        scheduledCollections.remove(collection);
    }

    public boolean taskExistsForCollection(Collection collection) {
        return scheduledCollections.containsKey(collection);
    }

    public ScheduledTask getTaskForCollection(Collection collection) {
        return scheduledCollections.get(collection);
    }
}
