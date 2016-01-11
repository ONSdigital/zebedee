package com.github.onsdigital.zebedee.content.page.staticpage.base;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.List;

/**
 * Created by bren on 29/06/15.
 */
public abstract class BaseStaticPage extends Page {

    private List<DownloadSection> downloads;

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

    public List<DownloadSection> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<DownloadSection> downloads) {
        this.downloads = downloads;
    }
}
