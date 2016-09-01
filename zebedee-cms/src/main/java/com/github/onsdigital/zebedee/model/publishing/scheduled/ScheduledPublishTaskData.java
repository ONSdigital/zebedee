package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.json.CollectionBase;

import java.util.Date;
import java.util.Set;

public class ScheduledPublishTaskData {
    public long delay;
    public Date scheduledPublishDate;
    public Set<String> collectionIds;
    public Date expectedPublishDate;
    public Set<CollectionBase> collections;
}
