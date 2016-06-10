package com.github.onsdigital.zebedee.model;

/**
 * Type representing which audience the collection can be viewed by.
 */
public enum CollectionOwner {

    /**
     * Publishing support team.
     */
    PUBLISHING_SUPPORT("PST"),

    /**
     * Data Visualisation Team.
     */
    DATA_VISUALISATION("Data Visualisation");

    private final String displayText;

    CollectionOwner(String displayText) {
        this.displayText = displayText;
    }

    public String getDisplayText() {
        return displayText;
    }
}
