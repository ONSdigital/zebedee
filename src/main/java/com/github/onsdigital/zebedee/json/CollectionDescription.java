package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.Collection;

import java.util.Date;
import java.util.List;

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
    public List<String> approvedUris;

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
