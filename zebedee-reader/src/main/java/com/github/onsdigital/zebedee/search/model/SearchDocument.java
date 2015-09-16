package com.github.onsdigital.zebedee.search.model;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.net.URI;

/**
 * Created by bren on 03/09/15.
 */
public class SearchDocument {
    private URI uri;
    private PageType type;
    private PageDescription description;

    public PageDescription getDescription() {
        return description;
    }

    public void setDescription(PageDescription description) {
        this.description = description;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public PageType getType() {
        return type;
    }

    public void setType(PageType type) {
        this.type = type;
    }
}
