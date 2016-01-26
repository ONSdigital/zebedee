package com.github.onsdigital.zebedee.search.model;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.net.URI;
import java.util.List;

/**
 * Created by bren on 03/09/15.
 */
public class SearchDocument {
    private URI uri;
    private PageType type;
    private PageDescription description;
    private List<URI> topics;
    private List<String> searchBoost;


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

    public List<URI> getTopics() {
        return topics;
    }

    public void setTopics(List<URI> topics) {
        this.topics = topics;
    }

    public List<String> getSearchBoost() {
        return searchBoost;
    }

    public void setSearchBoost(List<String> searchBoost) {
        this.searchBoost = searchBoost;
    }
}
