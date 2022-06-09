package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.files.api.Client;
import com.github.onsdigital.zebedee.model.Collection;

public class StaticFilesServiceImpl implements StaticFilesService {

    private final Client client;

    public StaticFilesServiceImpl(Client client) {
        this.client = client;
    }

    @Override
    public void publishCollection(Collection collection){
        if (collection.getDescription() == null) {
            throw new IllegalArgumentException("collection description was null");
        }

        String collectionId = collection.getDescription().getId();

        if (collectionId == null || collectionId.isEmpty()) {
            throw new IllegalArgumentException("a collectionId must be set in the collection being published");
        }

        client.publishCollection(collectionId);
    }
}
