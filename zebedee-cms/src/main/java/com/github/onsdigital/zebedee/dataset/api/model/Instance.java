package com.github.onsdigital.zebedee.dataset.api.model;

/**
 * The model of an instance as provided by the dataset API.
 */
public class Instance {

    private String id;
    private String edition;
    private String version;
    private Links links;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    public static class Links {
        public Link dataset;
    }
}
