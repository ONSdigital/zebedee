package com.github.onsdigital.zebedee.json;

public class CollectionInteractiveFile {

    public String name;
    public String URI;

    public CollectionInteractiveFile(String name, String URI) {
        this.name = name;
        this.URI = URI;
    }

    public String getName() { return name; }
    public String getURI() { return URI; }

    public void setURI(String URI) { this.URI = URI; }
    public void setName(String name) { this.name = name; }
}
