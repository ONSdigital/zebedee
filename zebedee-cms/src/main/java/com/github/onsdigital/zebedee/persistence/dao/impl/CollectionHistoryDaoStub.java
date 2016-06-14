package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;

import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Mock implementation of {@link CollectionHistoryDao} for use while actual DB is still being set up and for testing.
 */
public class CollectionHistoryDaoStub implements CollectionHistoryDao {

    private static final List<CollectionHistoryEvent> emptyList = new ArrayList<>();

    @Override
    public void saveCollectionHistoryEvent(CollectionHistoryEvent event) throws ZebedeeException {
        logInfo(event.toString());
    }

    @Override
    public void saveCollectionHistoryEvent(Collection collection, Session session,
                                           CollectionEventType collectionEventType, CollectionEventMetaData... metaValues)
            throws ZebedeeException {
        this.saveCollectionHistoryEvent(new CollectionHistoryEvent(collection, session, collectionEventType, metaValues));
    }

    @Override
    public void saveCollectionHistoryEvent(String collectionName, String collectionId, Session session,
                                           CollectionEventType collectionEventType, CollectionEventMetaData... metaValues)
            throws ZebedeeException {
        this.saveCollectionHistoryEvent(new CollectionHistoryEvent(collectionId, collectionName, session, collectionEventType,
                metaValues));
    }

    @Override
    public List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException {
        logInfo("getCollectionEventHistory: AUDIT database is not enabled Events are written to application log " +
                "only.");
        return emptyList;
    }
}
