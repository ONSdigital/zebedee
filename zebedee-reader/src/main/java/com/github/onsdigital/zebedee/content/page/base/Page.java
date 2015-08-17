package com.github.onsdigital.zebedee.content.page.base;

import com.github.onsdigital.zebedee.content.base.Content;

import java.net.URI;

/**
 * Created by bren on 10/06/15.
 * <p>
 * This is the generic content object that that has common properties of all page types on the website
 */
public abstract class Page extends Content {

    private URI uri;

    private PageDescription description;

    public abstract PageType getType();

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
}
