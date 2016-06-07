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

    COLLECTION_EDITED_PUBLISH_RESCHEDULED("collection.publish.rescheduled.description"),

    COLLECTION_EDITED_VIEWER_TEAM_ADDED("collection.viewer.team.added.description"),

    COLLECTION_EDITED_VIEWER_TEAM_REMOVED("collection.viewer.team.removed.description"),

    COLLECTION_EDITED_TYPE_CHANGED("collection.type.changed.description"),

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
