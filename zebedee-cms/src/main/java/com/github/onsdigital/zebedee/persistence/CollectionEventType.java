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
     * Collection name changed.
     */
    COLLECTION_NAME_CHANGED("collection.name.changed.description"),

    /**
     * Collection publish rescheduled.
     */
    COLLECTION_PUBLISH_RESCHEDULED("collection.publish.rescheduled.description"),

    /**
     * Viewer team added to collection.
     */
    COLLECTION_VIEWER_TEAM_ADDED("collection.viewer.team.added.description"),

    /**
     * Viewer team removed from collection.
     */
    COLLECTION_VIEWER_TEAM_REMOVED("collection.viewer.team.removed.description"),

    /**
     * Collection type({@link com.github.onsdigital.zebedee.json.CollectionType#manual} /
     * {@link com.github.onsdigital.zebedee.json.CollectionType#scheduled}) changed.
     */
    COLLECTION_TYPE_CHANGED("collection.type.changed.description"),

    /**
     * Page added to collection.
     */
    COLLECTION_PAGE_SAVED("collection.page.saved.description"),

    /**
     * File added.
     */
    COLLECTION_FILE_SAVED("collection.file.saved.description"),

    /**
     * Collection deleted.
     */
    COLLECTION_DELETED("collection.delete.description"),

    /**
     * Collection approved.
     */
    COLLECTION_APPROVED("collection.approved.description"),

    /**
     * Collection unlocked.
     */
    COLLECTION_UNLOCKED("collection.unlocked.description"),

    /**
     * Collection page moved to completed state.
     */
    COLLECTION_ITEM_COMPLETED("collection.completed.description"),

    /**
     * Collection published.
     */
    COLLECTION_POST_PUBLISHED_CONFIRMATION("collection.post.publish.confirmation.description"),

    /**
     * Collection manual publish invoked.
     */
    COLLECTION_MANUAL_PUBLISHED_TRIGGERED("collection.manual.publish.triggered.description"),

    /**
     * Collection manual publish complete successfully.
     */
    COLLECTION_MANUAL_PUBLISHED_SUCCESS("collection.manual.publish.success.description"),

    /**
     * Collection manual publish complete successfully.
     */
    COLLECTION_MANUAL_PUBLISHED_FAILURE("collection.manual.publish.failure.description"),

    /**
     * Collection table modified - rows/columns excluded or header rows/cols added.
     */
    COLLECTION_TABLE_MODIFIED("collection.table.file.modified.description"),

    /**
     * Collection table created.
     */
    COLLECTION_TABLE_CREATED("collection.table.created.description"),

    /**
     * Error while attemption to move collection page completed state.
     */
    COLLECTION_COMPLETED_ERROR("collection.completed.error.description"),

    /**
     * Collect page/item deleted.
     */
    COLLECTION_CONTENT_DELETED("collection.content.deleted.description"),

    /**
     * Data visualisation zip unpacked.
     */
    DATA_VISUALISATION_ZIP_UNPACKED("data.visualisation.zip.unpacked.description"),

    /**
     * Collect page/item deleted.
     */
    DATA_VISUALISATION_COLLECTION_CONTENT_DELETED("data.visualisation.collection.content.deleted.description"),

    /**
     * Collect page/item reviewed.
     */
    COLLECTION_CONTENT_REVIEWED("collection.content.reviewed.description"),

    /**
     * Collect page/item renamed.
     */
    COLLECTION_CONTENT_RENAMED("collection.content.renamed.description"),

    /**
     * Collection content moved.
     */
    COLLECTION_CONTENT_MOVED("collection.content.moved.description"),

    /**
     * Delete marker added to content.
     */
    DELETE_MARKED_ADDED("delete.marker.added.description"),

    /**
     * Delete marker removed from content.
     */
    DELETE_MARKED_REMOVED("delete.marker.removed.description");

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
