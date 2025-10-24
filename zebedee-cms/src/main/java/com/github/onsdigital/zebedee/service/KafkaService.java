package com.github.onsdigital.zebedee.service;

import java.io.IOException;
import java.util.List;

/**
 * Provides high level kafka functionality
 */
public interface KafkaService {

    /**
     * Produce kafka content-updated events for a collection
     *
     * @param uris         A list of uris modified/created in the collection
     * @param dataType     A string to identify if the list of uris are from
     *                     datasetAPI
     * @param collectionID The collection id of the collection being published
     * @param jobID        The job id of the collection being published
     * @param searchIndex  The search index of the collection being published
     * @param traceID      The trace id of the collection being published
     */
    void produceContentUpdated(String collectionID, List<String> uris, String dataType, String jobID,
            String searchIndex, String traceID) throws IOException;


    /**
     * Produce 'content-deleted' Kafka events for a collection.
     *
     * @param uris         A list of URIs that were deleted from publishing
     * @param collectionID The collection ID that triggered the event
     * @param indexName    The search index name where deletions should occur
     * @param traceID      Trace ID for correlation and logging
     */
    void produceContentDeleted(String collectionID, List<String> uris, String indexName, String traceID) throws IOException;

}
