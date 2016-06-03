package com.github.onsdigital.zebedee.persistence;

/**
 * Constants representing the different types of collection history events.
 */
public enum CollectionEventType {

    COLLECTION_CREATED("collection.created.description"),

    COLLECTION_APPROVED("collection.approved.description"),

    COLLECTION_DELETED("collection.delete.description"),

    COLLECTION_UNLOCKED("collection.unlocked.description"),

    COLLECTION_PUBLISHED("collection.published.description"),

    COLLECTION_EDITED_NAME_CHANGED("collection.name.changed.description"),

    COLLECTION_EDITED_PUBLISH_RESCHEDULED("collection.publish.changed.description"),

    COLLECTION_EDITED_TEAM_ADDED("collection.team.added.description"),

    COLLECTION_EDITED_TEAM_REMOVED("collection.team.removed.description"),

    PAGE(""),

    FILE("");

    private final String descriptionKey;

    /**
     * @param descriptionKey the properties key to use when fetching the description value.
     */
    CollectionEventType(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }


}
