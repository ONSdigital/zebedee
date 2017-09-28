package com.github.onsdigital.zebedee.dataset.api;

/**
 * The link structure used by the dataset API.
 */
public class Link {

    private String id;
    private String href;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
