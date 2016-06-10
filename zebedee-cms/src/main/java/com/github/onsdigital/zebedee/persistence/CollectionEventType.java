package com.github.onsdigital.zebedee.persistence;

/**
 * Constants representing the different types of collection history events.
 */
public enum CollectionEventType {

    /**
     * Collection created.
     */
    COLLECTION_CREATED("collection.created.description"),

    /**
     * Collection approved.
     */
    COLLECTION_APPROVED("collection.approved.description"),

    /**
     * Collection deleted.
     */
    COLLECTION_DELETED("collection.delete.description"),

    /**
     * Collection unlocked.
     */
    COLLECTION_UNLOCKED("collection.unlocked.description"),

    /**
     * Collection published.
     */
    COLLECTION_PUBLISHED("collection.published.description"),

    /**
     * Collection name changed.
     */
    COLLECTION_EDITED_NAME_CHANGED("collection.name.changed.description"),

    /**
     * Collection publish rescheduled.
     */
    COLLECTION_EDITED_PUBLISH_RESCHEDULED("collection.publish.rescheduled.description"),

    /**
     * Viewer team added to collection.
     */
    COLLECTION_EDITED_VIEWER_TEAM_ADDED("collection.viewer.team.added.description"),

    /**
     * Viewer team removed from collection.
     */
    COLLECTION_EDITED_VIEWER_TEAM_REMOVED("collection.viewer.team.removed.description"),

    /**
     * Collection type({@link com.github.onsdigital.zebedee.json.CollectionType#manual} /
     * {@link com.github.onsdigital.zebedee.json.CollectionType#scheduled}) changed.
     */
    COLLECTION_EDITED_TYPE_CHANGED("collection.type.changed.description"),

    /**
     * Page added to collection.
     */
    COLLECTION_PAGE_ADDED("collection.page.added.description"),

    /**
     * Page updated or saved.
     */
    COLLECTION_PAGE_MODIFIED("collection.page.modified.description"),

    /**
     * File added.
     */
    COLLECTION_FILE_ADDED("collection.file.added.description"),

    /**
     * File updated.
     */
    COLLECTION_FILE_MODIFIED("collection.file.modified.description"),

    /**
     * Collection table modified - rows/columns excluded or header rows/cols added.
     */
    COLLECTION_TABLE_MODIFIED("collection.table.file.modified.description"),

    /**
     * Collection page moved to completed state.
     */
    COLLECTION_ITEM_COMPLETED("collection.completed.description"),

    /**
     * Error while attemption to move collection page completed state.
     */
    COLLECTION_COMPLETED_ERROR("collection.completed.error.description"),

    /**
     * Collect page/item deleted.
     */
    COLLECTION_CONTENT_DELETED("collection.content.deleted.description"),

    /**
     * Collect page/item deleted.
     */
    DATA_VISUALISATION_COLLECTION_CONTENT_DELETED("data.visualisation.collection.content.deleted.description");

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
