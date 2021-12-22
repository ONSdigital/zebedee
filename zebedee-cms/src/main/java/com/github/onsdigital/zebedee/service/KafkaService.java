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
     * @param dataType A string to identify if the list of uris are from datasetAPI
     */
    void produceContentPublished(String collectionID, List<String> uris, String dataType) throws IOException;
}
