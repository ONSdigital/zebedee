package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;

public class PublishTask implements Runnable {

    private final String collectionId;
    private final Zebedee zebedee;

    public PublishTask(Zebedee zebedee, Collection collection) {
        this.collectionId = collection.description.id;
        this.zebedee = zebedee;
    }

    @Override
    public void run() {
        System.out.println("Running scheduled job for collection id: " + collectionId);

        try {
            Collection collection = zebedee.collections.list().getCollection(this.collectionId);
            Publisher.Publish(zebedee, collection, "System");
        } catch (IOException e) {
            System.out.println("Exception publishing collection for ID" + collectionId + " exception:" + e.getMessage());
        }
    }
}
