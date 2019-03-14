package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

public abstract class Scheduler {

    public void schedulePublish(Collection collection, Zebedee zebedee) {
        if (Configuration.isSchedulingEnabled()) {
            try {
                info().data("collectionId", collection.getDescription().getId())
                        .data("collectionType", collection.getDescription().getType())
                        .log("Attempting collection schedule publish");
                if (collection.getDescription().getType() == CollectionType.scheduled) {
                    schedule(collection, zebedee);
                }
            } catch (Exception e) {
                error().data("collectionId", collection.getDescription().getId())
                        .logException(e, "Exception caught trying to schedule existing collection");
            }
        } else {
            info().data("collectionId", collection.getDescription().getId())
                    .log("Not scheduling collection, scheduling is not enabled");
        }
    }

    protected abstract void schedule(Collection collection, Zebedee zebedee);

    public abstract void cancel(Collection collection);
}
