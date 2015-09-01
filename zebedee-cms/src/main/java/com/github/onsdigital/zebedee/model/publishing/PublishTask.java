package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.model.Collection;

public class PublishTask implements Runnable {

    private final String collectionId;

    public PublishTask(Collection collection) {
        this.collectionId = collection.description.id;
    }

    @Override
    public void run() {
        System.out.println("Running scheduled job for collection id: " + collectionId);
    }
}
