package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;

import java.text.MessageFormat;

import static com.github.onsdigital.zebedee.logging.SimpleLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.SimpleLogBuilder.logMessage;

public abstract class Scheduler {

    public void schedulePublish(Collection collection, Zebedee zebedee) {
        if (Configuration.isSchedulingEnabled()) {
            try {
                logMessage("Attempting to schedule publish for collection " + collection.description.name + " type=" + collection.description.type);
                if (collection.description.type == CollectionType.scheduled) {
                    schedule(collection, zebedee);
                }
            } catch (Exception e) {
                logError("Exception caught trying to schedule existing collection: " + e.getMessage());
            }
        } else {
            logMessage(MessageFormat.format("Not scheduling collection {0}, scheduling is not enabled", collection.description.name));
        }
    }

    protected abstract void schedule(Collection collection, Zebedee zebedee);

    public abstract void cancel(Collection collection);
}
