package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.Log;

public abstract class Scheduler {

    public void schedulePublish(Collection collection, Zebedee zebedee) {
        if (Configuration.isSchedulingEnabled()) {
            try {
                System.out.println("Attempting to schedule publish for collection " + collection.description.name + " type=" + collection.description.type);
                if (collection.description.type == CollectionType.scheduled) {
                    schedule(collection, zebedee);
                }
            } catch (Exception e) {
                System.out.println("Exception caught trying to schedule existing collection: " + e.getMessage());
            }
        } else {
            Log.print("Not scheduling collection %s, scheduling is not enabled", collection.description.name);
        }
    }

    protected abstract void schedule(Collection collection, Zebedee zebedee);

    public abstract void cancel(Collection collection);
}
