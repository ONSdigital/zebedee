package com.github.onsdigital.zebedee.json;

public class CollectionInteractiveFile {

    public String name;
    public String uri;

    public CollectionInteractiveFile(String name, String uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() { return name; }
    public String getUri() { return uri; }

    public void setUri(String uri) { this.uri = uri; }
    public void setName(String name) { this.name = name; }
}
