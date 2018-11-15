package com.github.onsdigital.zebedee.search.fastText.response;

import java.util.Map;

public class SentenceVectorResponse {

    private String vector;
    private Map<String, Float> keywords;

    private SentenceVectorResponse() {
        // For Jackson
    }

    public String getVector() {
        return vector;
    }

    public Map<String, Float> getKeywords() {
        return keywords;
    }
}
