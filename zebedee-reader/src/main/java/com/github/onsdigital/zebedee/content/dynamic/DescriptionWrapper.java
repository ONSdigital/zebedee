package com.github.onsdigital.zebedee.content.dynamic;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;

import java.net.URI;

/**
 * Created by bren on 15/08/15.
 */
public class DescriptionWrapper extends Content {

    private URI uri;
    private PageDescription description;

    public DescriptionWrapper(URI uri, PageDescription description) {
        this.uri = uri;
        this.description = description;
    }

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
