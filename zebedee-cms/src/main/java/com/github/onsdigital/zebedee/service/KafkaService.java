package com.github.onsdigital.zebedee.service;

import java.io.IOException;
import java.util.List;

/**
 * Provides high level kafka functionality
 */
public interface KafkaService {

    /**
     * Produce kafka content-published events for a collection
     *
     * @param collectionID The id of the collection being published
     * @param uris A list of uris modified/created in the collection
     */
    void produceContentPublished(String collectionID, List<String> uris) throws IOException;
}
