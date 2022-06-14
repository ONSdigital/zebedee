package com.github.onsdigital.zebedee.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.Future;

/**
 * Interface representing a client that sends messages to kafka
 */
public interface KafkaClient {

    /**
     * Produce a 'content-updated' kafka message for the given parameters
     * 
     * @param uri          The url of the published resource
     * @param dataType     The data type of the published resource
     * @param collectionID the ID of the collection being published
     * @param jobID        the ID of the job being published
     * @param searchIndex  the ID of the searchIndex being published
     * @param traceID      of the collection being published
     * @return
     */
    public Future<RecordMetadata> produceContentUpdated(String uri, String dataType, String collectionID,
            String jobID, String searchIndex, String traceID);
}
