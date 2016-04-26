package com.github.onsdigital.zebedee.model;

/**
 * Define the interface for writing to collections, i.e. the available folders inprogress, complete, and reviewed.
 */
public abstract class CollectionWriter {

    protected ContentWriter inProgress;
    protected ContentWriter complete;
    protected ContentWriter reviewed;
    protected ContentWriter root;

    public ContentWriter getInProgress() {
        return inProgress;
    }

    public ContentWriter getComplete() {
        return complete;
    }

    public ContentWriter getReviewed() {
        return reviewed;
    }

    public ContentWriter getRoot() {
        return root;
    }
}
