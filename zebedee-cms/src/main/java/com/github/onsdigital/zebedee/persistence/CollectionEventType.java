package com.github.onsdigital.zebedee.persistence;

import java.util.Arrays;
import java.util.Optional;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Constants representing the different types of collection history events.
 */
public enum CollectionEventType {

    /**
     * Collection created.
     */
    COLLECTION_CREATED("collection.created.description", 0),

    /**
     * Collection name changed.
     */
    COLLECTION_NAME_CHANGED("collection.name.changed.description", 1),

    /**
     * Collection publish rescheduled.
     */
    COLLECTION_PUBLISH_RESCHEDULED("collection.publish.rescheduled.description", 2),

    /**
     * Viewer team added to collection.
     */
    COLLECTION_VIEWER_TEAM_ADDED("collection.viewer.team.added.description", 3),

    /**
     * Viewer team removed from collection.
     */
    COLLECTION_VIEWER_TEAM_REMOVED("collection.viewer.team.removed.description", 4),

    /**
     * Collection type({@link com.github.onsdigital.zebedee.json.CollectionType#manual} /
     * {@link com.github.onsdigital.zebedee.json.CollectionType#scheduled}) changed.
     */
    COLLECTION_TYPE_CHANGED("collection.type.changed.description", 5),

    /**
     * Page added to collection.
     */
    COLLECTION_PAGE_SAVED("collection.page.saved.description", 6),

    /**
     * File added.
     */
    COLLECTION_FILE_SAVED("collection.file.saved.description", 7),

    /**
     * Collection deleted.
     */
    COLLECTION_DELETED("collection.delete.description", 8),

    /**
     * Collection approved.
     */
    COLLECTION_APPROVED("collection.approved.description", 9),

    /**
     * Collection unlocked.
     */
    COLLECTION_UNLOCKED("collection.unlocked.description", 10),

    /**
     * Collection page moved to completed state.
     */
    COLLECTION_ITEM_COMPLETED("collection.completed.description", 11),

    /**
     * Collection published.
     */
    COLLECTION_POST_PUBLISHED_CONFIRMATION("collection.post.publish.confirmation.description", 12),

    /**
     * Collection manual publish invoked.
     */
    COLLECTION_MANUAL_PUBLISHED_TRIGGERED("collection.manual.publish.triggered.description", 13),

    /**
     * Collection manual publish complete successfully.
     */
    COLLECTION_MANUAL_PUBLISHED_SUCCESS("collection.manual.publish.success.description", 14),

    /**
     * Collection manual publish complete successfully.
     */
    COLLECTION_MANUAL_PUBLISHED_FAILURE("collection.manual.publish.failure.description", 15),

    /**
     * Collection table modified - rows/columns excluded or header rows/cols added.
     */
    COLLECTION_TABLE_MODIFIED("collection.table.file.modified.description", 16),

    /**
     * Collection table created.
     */
    COLLECTION_TABLE_CREATED("collection.table.created.description", 17),

    /**
     * Error while attemption to move collection page completed state.
     */
    COLLECTION_COMPLETED_ERROR("collection.completed.error.description", 18),

    /**
     * Collect page/item deleted.
     */
    COLLECTION_CONTENT_DELETED("collection.content.deleted.description", 19),

    /**
     * Data visualisation zip unpacked.
     */
    DATA_VISUALISATION_ZIP_UNPACKED("data.visualisation.zip.unpacked.description", 20),

    /**
     * Collect page/item deleted.
     */
    DATA_VISUALISATION_COLLECTION_CONTENT_DELETED("data.visualisation.collection.content.deleted.description", 21),

    /**
     * Collect page/item reviewed.
     */
    COLLECTION_CONTENT_REVIEWED("collection.content.reviewed.description", 22),

    /**
     * Collect page/item renamed.
     */
    COLLECTION_CONTENT_RENAMED("collection.content.renamed.description", 23),

    /**
     * Collection content moved.
     */
    COLLECTION_CONTENT_MOVED("collection.content.moved.description", 24),

    /**
     * Delete marker added to content.
     */
    DELETE_MARKED_ADDED("delete.marker.added.description", 25),

    /**
     * Delete marker removed from content.
     */
    DELETE_MARKED_REMOVED("delete.marker.removed.description", 26),

    UNSPECIFIED("unspecified.action.description", 666);

    private final String descriptionKey;
    private final int id;

    /**
     * @param descriptionKey the properties key to use when fetching the description value.
     */
    CollectionEventType(String descriptionKey, int id) {
        this.descriptionKey = descriptionKey;
        this.id = id;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public int getId() {
        return id;
    }

    public static CollectionEventType getById(int id) {
        Optional<CollectionEventType> result = Arrays.asList(CollectionEventType.values())
                .stream().filter(t -> id == t.id)
                .findFirst();

        if (result.isPresent()) {
            return result.get();
        }
        logInfo("failed to identify CollectionEventType by ID, returning CollectionEventType.UNSPECIFIED").param("ID", id).log();
        return UNSPECIFIED;
    }
}
