package com.github.onsdigital.zebedee.util.publish.pipeline;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.model.Collection;

public class Scheduler {

    private static final PublishCollection publishCollection = new PublishCollection();

    public static void schedule(Collection collection) {
        publishCollection.schedule(collection, Root.zebedee);
    }

    public static void cancel(Collection collection) {
        publishCollection.cancel(collection);
    }

}
