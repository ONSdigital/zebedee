package com.github.onsdigital.zebedee.reader.model;

import java.net.URI;

/**
 * Created by bren on 28/07/15.
 *
 * Represents a base content type that lives in content storage
 */
public abstract class Content {

    private URI uri;
    private String name;

    public Content(URI uri, String name) {
        this.uri = uri;
        this.name = name;
    }

    /**
     * @return uri that identifies this content uniquely
     */
    public URI getUri() {
        return uri;
    }

    /**
     *
     * @return name of the content
     */
    public String getName() {
        return name;
    }
}
