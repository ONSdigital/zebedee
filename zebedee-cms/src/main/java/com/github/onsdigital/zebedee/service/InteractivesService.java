package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionInteractive;
import com.github.onsdigital.zebedee.model.Collection;
import dp.api.dataset.exception.DatasetAPIException;

import java.io.IOException;

/**
 * Provides high level dataset functionality
 */
public interface InteractivesService {

    /**
     * Update a dataset for the given ID to the collection for the given collectionID.
     */
    CollectionInteractive updateInteractiveInCollection(Collection collection, String id, CollectionInteractive updatedInteractive, String user) throws ZebedeeException, IOException, RuntimeException;
    void removeInteractiveFromCollection(Collection collection, String interactiveID) throws IOException, RuntimeException;
    void publishCollection(Collection collection) throws RuntimeException;
}
