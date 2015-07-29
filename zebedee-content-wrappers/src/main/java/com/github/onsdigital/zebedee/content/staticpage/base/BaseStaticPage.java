package com.github.onsdigital.zebedee.content.staticpage.base;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.link.Link;

import java.util.List;

/**
 * Created by bren on 29/06/15.
 */
public abstract class BaseStaticPage extends Content {

    private List<Link> downloads;

    /**
     *Body in markdown format
     */
    private List<String> markdown;

    /**
     * Optional external links
     */
    private List<Link> links;

    public List<String> getMarkdown() {
        return markdown;
    }

    public void setMarkdown(List<String> markdown) {
        this.markdown = markdown;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<Link> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<Link> downloads) {
        this.downloads = downloads;
    }
}
