package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.debugMessage;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

public abstract class Scheduler {

    public void schedulePublish(Collection collection, Zebedee zebedee) {
        if (Configuration.isSchedulingEnabled()) {
            try {
                debugMessage("Attempting collection schedule publish")
                        .collectionName(collection)
                        .addParameter("collectionType", collection.description.type)
                        .log();
                if (collection.description.type == CollectionType.scheduled) {
                    schedule(collection, zebedee);
                }
            } catch (Exception e) {
                logError(e).errorContext("Exception caught trying to schedule existing collection")
                        .collectionName(collection)
                        .log();
            }
        } else {
            debugMessage("Not scheduling collection, scheduling is not enabled")
                    .collectionName(collection)
                    .log();
        }
    }

    protected abstract void schedule(Collection collection, Zebedee zebedee);

    public abstract void cancel(Collection collection);
}
