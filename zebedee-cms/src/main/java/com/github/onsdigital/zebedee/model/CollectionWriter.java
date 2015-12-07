package com.github.onsdigital.zebedee.model;

public abstract class CollectionWriter {

    protected ContentWriter inProgress;
    protected ContentWriter complete;
    protected ContentWriter reviewed;

    public ContentWriter getInProgress() {
        return inProgress;
    }

    public ContentWriter getComplete() {
        return complete;
    }

    public ContentWriter getReviewed() {
        return reviewed;
    }
}
