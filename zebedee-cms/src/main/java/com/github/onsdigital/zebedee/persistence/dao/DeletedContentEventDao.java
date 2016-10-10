package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.model.content.deleted.DeletedContentEvent;

import java.util.List;

/**
 * Data access interface for deleted content metadata.
 */
public interface DeletedContentEventDao {

    /**
     * Add a new deleted content event to the database.
     *
     * @param deletedContentEvent
     */
    DeletedContentEvent saveDeletedContentEvent(DeletedContentEvent deletedContentEvent);

    /**
     * Return a list of deleted content events.
     *
     * @return
     */
    List<DeletedContentEvent> listDeletedContent();

    /**
     * Get the deleted content event and the files that were deleted.
     * @param deletedContentEventId
     * @return
     */
    DeletedContentEvent retrieveDeletedContent(long deletedContentEventId);
}
