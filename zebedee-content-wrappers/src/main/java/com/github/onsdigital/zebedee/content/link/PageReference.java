package com.github.onsdigital.zebedee.content.link;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.net.URI;

/**
 * Created by bren on 04/06/15.
 * <p>
 * Reference to pages. Contains the uri to referenced page and also provides a mechanism to lazy load the description or full data of  referred page
 */
public class PageReference extends Content implements Comparable<PageReference> {

    /**
     * URI should be immutable. If uri of a page changes the old uri should be redirected to new one. This should not be changed
     */
    private URI uri;
    //Index used for ordering
    private Integer index;
    //Description or full page may be lazy loaded
    private PageDescription description;
    private Page data;
    private PageType type;

    /**
     * @param uri of referenced content
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

    public PageDescription getDescription() {
        return description;
    }

    public void setDescription(PageDescription description) {
        this.description = description;
    }

    public Page getData() {
        return data;
    }

    public void setData(Page data) {
        this.data = data;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public PageType getType() {
        return type;
    }

    public void setType(PageType type) {
        this.type = type;
    }
}
