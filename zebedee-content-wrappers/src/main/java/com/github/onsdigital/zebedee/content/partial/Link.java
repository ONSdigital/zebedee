package com.github.onsdigital.zebedee.content.partial;

import com.github.onsdigital.zebedee.content.base.Content;

import java.net.URI;

/**
 * Created by bren on 30/06/15.
 * <p/>
 * Represents any link on website
 */
public class Link extends Content implements Comparable<Link> {

    private String title;
    private URI uri;
    //Index used for ordering
    private Integer index;

    public Link(URI uri) {
        this(uri, null);
    }

    /**
     * Creates the reference to given page using only uri of the page
     *
     * @param uri
     * @param index Index used for odering of links when set
     */
    public Link(URI uri, Integer index) {
        this.index = index;
        setUri(uri);
    }


    @Override
    public int compareTo(Link o) {
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


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

}
