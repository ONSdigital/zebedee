package com.github.onsdigital.zebedee.dataset.api;

/**
 * The model of a dataset as provided by the dataset API.
 */
public class Dataset {

    private String id;
    private String title;
    private String uri;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}

