package com.github.onsdigital.zebedee.collection.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class Collection {

    private String id;
    private String name;
    private CollectionType type;
    private CollectionStatus status;
    private Date publishDate;
    private String releaseUri;

    private Set<CollectionContentItem> collectionContent;
    private List<CollectionEvent> events;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CollectionType getType() {
        return type;
    }

    public CollectionStatus getStatus() {
        return status;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public String getReleaseUri() {
        return releaseUri;
    }
}
