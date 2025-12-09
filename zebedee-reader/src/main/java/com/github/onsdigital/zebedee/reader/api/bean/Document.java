package com.github.onsdigital.zebedee.reader.api.bean;

/**
 * Created by bren on 25/01/16.
 *  */
public class Document {
    private String uri;

    public Document(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
