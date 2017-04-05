package com.github.onsdigital.zebedee.model.content.deleted;

/**
 * Created by carlhembrough on 07/10/2016.
 */
public class DeletedFile {
    private long id;
    private String uri;

    public DeletedFile(String uri) {
        this.uri = uri;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
