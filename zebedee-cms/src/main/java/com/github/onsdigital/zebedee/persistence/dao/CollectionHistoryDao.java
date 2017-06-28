package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Defines API of a CollectionHistoryDao.
 */
public interface CollectionHistoryDao {

    /**
     * Save a collection audit history event.
     *
     * @param event a {@link CollectionHistoryEvent} containing the specific details of the event to save.
     * @throws ZebedeeException unexpected problem saving the event.
     */
    Future saveCollectionHistoryEvent(CollectionHistoryEvent event);


    Future saveCollectionHistoryEvent(Collection collection, Session session,
                                      CollectionEventType collectionEventType,
                                      CollectionEventMetaData... metaValues);


    Future saveCollectionHistoryEvent(String collectionName, String collectionId, Session session,
                                      CollectionEventType collectionEventType,
                                      CollectionEventMetaData... metaValues);

    /**
     * Get the CollectionEventHistory of the specific collection.
     *
     * @param collectionId the collectionID of the collection to get the history for.
     * @return a {@link List} of @{@link CollectionHistoryEvent}'s making up the history of this collection.
     * @throws ZebedeeException unexpected error while attempting to obtain the collection history.
     */
    List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException;
}
