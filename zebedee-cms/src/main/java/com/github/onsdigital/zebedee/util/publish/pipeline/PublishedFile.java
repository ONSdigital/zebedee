package com.github.onsdigital.zebedee.util.publish.pipeline;

public class PublishedFile {

    private String uri;

    private String location;

    public PublishedFile(String uri, String fileLocation) {
        this.uri = uri;
        this.location = fileLocation;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
