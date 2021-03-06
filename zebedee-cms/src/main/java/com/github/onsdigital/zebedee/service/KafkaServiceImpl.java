package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.kafka.KafkaClient;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

public class KafkaServiceImpl implements KafkaService {
    private final KafkaClient kafkaClient;

    private static final String DATA_TYPE = "zebedee-content";

    public KafkaServiceImpl(KafkaClient kafkaClient) {
        this.kafkaClient = kafkaClient;
    }

    @Override
    public void produceContentPublished(String collectionId, List<String> uris) throws IOException {
        info().collectionID(collectionId)
                .data("uris", uris)
                .log("generating content-published kafka events for published collection");

        List<Future<RecordMetadata>> futureList = uris.stream()
                .map((uri) -> kafkaClient.produceContentPublished(uri, DATA_TYPE, collectionId))
                .collect(Collectors.toList());


        for (Future<RecordMetadata> future : futureList) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                error().collectionID(collectionId).exception(e).log("unable to process kafka message");
                throw new IOException("unable to process kafka message", e);
            }
        }
    }
}
