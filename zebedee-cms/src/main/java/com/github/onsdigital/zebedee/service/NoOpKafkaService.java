package com.github.onsdigital.zebedee.service;

import java.io.IOException;
import java.util.List;

/**
 * A no-op KafkaService that does nothing. This is used for the case where kafka is disabled via the feature flags.
 */
public class NoOpKafkaService implements KafkaService {

    @Override
    public void produceContentPublished(String collectionId, List<String> uris, String dataType) throws IOException {
        return;
    }
}
