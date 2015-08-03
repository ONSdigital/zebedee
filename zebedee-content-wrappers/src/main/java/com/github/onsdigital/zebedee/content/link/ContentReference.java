package com.github.onsdigital.zebedee.content.link;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentDescription;
import com.github.onsdigital.zebedee.content.base.ContentType;

import java.net.URI;

/**
 * Created by bren on 04/06/15.
 * <p>
 * Reference to pages. Contains the uri to referenced page and also provides a mechanism to lazy load the description or full data of  referred page
 */
public class ContentReference implements Comparable<ContentReference> {

    /**
     * URI should be immutable. If uri of a page changes the old uri should be redirected to new one. This should not be changed
     */
    private URI uri;
    //Index used for ordering
    private Integer index;
    //Description or full page may be lazy loaded
    private ContentDescription description;
    private Content data;
    private ContentType type;

    /**
     * @param uri of referenced content
     */
    public ContentReference(URI uri) {
        this(uri, null);
    }

    public ContentReference(URI uri, Integer index) {
        this.uri = uri;
        this.index = index;
    }

    @Override
    public int compareTo(ContentReference o) {
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

    public ContentDescription getDescription() {
        return description;
    }

    public void setDescription(ContentDescription description) {
        this.description = description;
    }

    public Content getData() {
        return data;
    }

    public void setData(Content data) {
        this.data = data;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public ContentType getType() {
        return type;
    }

    public void setType(ContentType type) {
        this.type = type;
    }
}
