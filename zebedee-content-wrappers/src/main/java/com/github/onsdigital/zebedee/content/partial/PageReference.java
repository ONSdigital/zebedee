package com.github.onsdigital.zebedee.content.partial;

import com.github.onsdigital.zebedee.content.base.Content;

import java.net.URI;

/**
 * Created by bren on 04/06/15.
 * <p>
 * Reference to pages
 */
public class PageReference extends Content implements Comparable<PageReference> {

    private URI uri;
    //Index used for ordering
    private Integer index;

    /**
     * @param uri of referenced page
     */
    public PageReference(URI uri) {
        this(uri, null);
    }

    public PageReference(URI uri, Integer index) {
        this.uri = uri;
        this.index = index;
    }

    @Override
    public int compareTo(PageReference o) {
        //nulls last or first
        if (this.index == null) {
            return -1;
        }
        return Integer.compare(this.index, o.index);
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
