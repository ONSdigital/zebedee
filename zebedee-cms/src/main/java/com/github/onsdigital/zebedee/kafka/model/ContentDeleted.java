package com.github.onsdigital.zebedee.kafka.model;

public class ContentDeleted {
    private String uri;
    private String collectionId;
    private String searchIndex;
    private String traceId;

    public ContentDeleted() {
    }

    public ContentDeleted(String uri, String collectionId, String searchIndex, String traceId) {
        this.uri = uri;
        this.collectionId = collectionId;
        this.searchIndex = searchIndex;
        this.traceId = traceId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getSearchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(String searchIndex) {
        this.searchIndex = searchIndex;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
