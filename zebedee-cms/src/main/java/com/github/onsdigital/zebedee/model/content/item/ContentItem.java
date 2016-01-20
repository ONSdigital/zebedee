package com.github.onsdigital.zebedee.model.content.item;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;

/**
 * Base class to represent a single item of content.
 * <p>
 * This typically consists of a directory that contains a data.json file.
 * It can also contain any files associated with the page.
 */
public class ContentItem {

    private final String uri;

    public ContentItem(String uri) throws NotFoundException {
        this.uri = uri;
    }

    /**
     * The URI of the content path as shown in the data.json
     * @return
     */
    public String getUri() {
        return uri;
    }
}
