package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionInteractive;
import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;

/**
 * Provides high level interactives functionality
 */
public interface InteractivesService {

    /**
     * Update an interactive for the given collection.
     * 
     * @param collection         The collection
     * @param id                 The interactive id
     * @param updatedInteractive The new interactive object
     * @param user               The user performing the action
     * @return The new amended interactive object
     * @throws ZebedeeException
     * @throws IOException
     */
    CollectionInteractive updateInteractiveInCollection(Collection collection, String id, CollectionInteractive updatedInteractive, String user) throws ZebedeeException, IOException;

    /**
     * Remove an interactive from the given collection
     * 
     * @param collection    The collection
     * @param interactiveID The interactive id
     * @throws ZebedeeException If an error occurs in the interactives api
     * @throws IOException      If an error occurs while saving the collection state
     */
    void removeInteractiveFromCollection(Collection collection, String id) throws ZebedeeException, IOException;

    /**
     * Notify the interactives api of a collection publication
     * 
     * @param collection The collection
     */
    void publishCollection(Collection collection);
}
