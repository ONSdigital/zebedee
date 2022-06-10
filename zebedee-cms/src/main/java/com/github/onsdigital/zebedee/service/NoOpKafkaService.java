package com.github.onsdigital.zebedee.service;

import java.io.IOException;
import java.util.List;

/**
 * A no-op KafkaService that does nothing. This is used for the case where kafka
 * is disabled via the feature flags.
 */
public class NoOpKafkaService implements KafkaService {

    @Override
    public void produceContentUpdated(String collectionId, List<String> uris, String dataType, String jobID,
            String searchIndex, String traceId) throws IOException {
        return;
    }
}
