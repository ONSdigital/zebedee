package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.model.Collection;

import java.util.Date;
import java.util.List;

public class CollectionDetail {
    /**
     * The unique identifier of this {@link Collection}.
     */
    public String id;
    /**
     * The readable name of this {@link Collection}.
     */
    public String name;
    /**
     * The type of the collection to determine the publish behaviour.
     */
    public CollectionType type;
    /**
     * The date-time when this {@link Collection} should be published (if it has
     * a publish date).
     */
    public Date publishDate;

    public List<ContentDetail> inProgress;
    public List<ContentDetail> complete;
    public List<ContentDetail> reviewed;
    public boolean approvedStatus;

    public Events events;
}
