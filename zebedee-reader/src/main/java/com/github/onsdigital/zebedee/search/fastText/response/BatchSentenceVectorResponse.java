package com.github.onsdigital.zebedee.search.fastText.response;

import java.util.Map;

public class BatchSentenceVectorResponse {

    private Map<String, SentenceVectorResponse> results;

    private BatchSentenceVectorResponse() {
        // For Jackson
    }

    public Map<String, SentenceVectorResponse> getResults() {
        return results;
    }
}
