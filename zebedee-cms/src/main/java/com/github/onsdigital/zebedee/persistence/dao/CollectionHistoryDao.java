package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;

import java.util.List;

/**
 * Defines API of a CollectionHistoryDao.
 */
public abstract class CollectionHistoryDao {

    private static CollectionHistoryDao instance = null;

    CollectionHistoryDao() {
        // Hide Constructor from the outside.
    }

    public static void setInstance(CollectionHistoryDao dao) {
        instance = dao;
    }

    public static CollectionHistoryDao getCollectionHistoryDao() {
        if (instance == null) {
            // Default to the test stub.
            instance = new CollectionHistoryDaoStub();
        }
        return instance;
    }

    /**
     * Save a collection audit history event.
     *
     * @param event a {@link CollectionHistoryEvent} containing the specific details of the event to save.
     * @throws ZebedeeException unexpected problem saving the event.
     */
    public abstract void saveCollectionHistoryEvent(CollectionHistoryEvent event) throws ZebedeeException;


    public abstract void saveCollectionHistoryEvent(Collection collection, Session session,
                                                    CollectionEventType collectionEventType,
                                                    CollectionEventMetaData... metaValues) throws ZebedeeException;


    public abstract void saveCollectionHistoryEvent(String collectionName, String collectionId, Session session,
                                                    CollectionEventType collectionEventType,
                                                    CollectionEventMetaData... metaValues) throws ZebedeeException;

    /**
     * Get the CollectionEventHistory of the specific collection.
     *
     * @param collectionId the collectionID of the collection to get the history for.
     * @return a {@link List} of @{@link CollectionHistoryEvent}'s making up the history of this collection.
     * @throws ZebedeeException unexpected error while attempting to obtain the collection history.
     */
    public abstract List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException;
}
