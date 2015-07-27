package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.model.Collection;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This cd ..
 *
 * @author david
 */
public class CollectionDescription {

    /**
     * The unique identifier of this {@link Collection}.
     */
    public String id;
    /**
     * The readable name of this {@link Collection}.
     */
    public String name;
    /**
     * The date-time when this {@link Collection} should be published (if it has
     * a publish date).
     */
    public Date publishDate;

    public List<String> inProgressUris;
    public List<String> completeUris;
    public List<String> reviewedUris;
    public boolean approvedStatus;

    /**
     * A List of {@link ContentEvent} for each uri in the collection.
     */
    public Map<String, ContentEvents> eventsByUri;

    /**
     * Default constuructor for serialisation.
     */
    public CollectionDescription() {
        // No action.
    }

    /**
     * Convenience constructor for instantiating with a name.
     *
     * @param name The value for the name.
     */
    public CollectionDescription(String name) {
        this.name = name;
    }

    /**
     * Convenience constructor for instantiating with a name
     * and publish date.
     *
     * @param name
     * @param publishDate
     */
    public CollectionDescription(String name, Date publishDate) {
        this.publishDate = publishDate;
        this.name = name;
    }


}
