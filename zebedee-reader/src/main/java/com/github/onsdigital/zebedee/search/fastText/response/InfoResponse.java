package com.github.onsdigital.zebedee.search.fastText.response;

import org.codehaus.jackson.annotate.JsonProperty;

public class InfoResponse {

    private int dimensions;

    @JsonProperty("isQuantised")
    private boolean isQuantised;

    private InfoResponse() {
        // For Jackson
    }

    public int getDimensions() {
        return dimensions;
    }

    @JsonProperty("isQuantised")
    public boolean isQuantised() {
        return isQuantised;
    }
}
