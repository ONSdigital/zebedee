package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;

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
            boolean publishComplete = Publisher.Publish(collection, "System");

            if (publishComplete) {
                Collections.MoveFilesToMaster(zebedee, collection);
                Collections.MoveCollectionToArchive(zebedee, collection);

                // Delete the folders:
                collection.delete();
            }
        } catch (IOException e) {
            System.out.println("Exception publishing collection for ID" + collectionId + " exception:" + e.getMessage());
        }

    }
}
