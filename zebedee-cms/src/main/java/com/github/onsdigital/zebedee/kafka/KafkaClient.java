package com.github.onsdigital.zebedee.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.Future;

/**
 * Interface representing a client that sends messages to kafka
 */
public interface KafkaClient {
    public Future<RecordMetadata> produceContentPublished(String url, String dataType, String collectionID);
}
