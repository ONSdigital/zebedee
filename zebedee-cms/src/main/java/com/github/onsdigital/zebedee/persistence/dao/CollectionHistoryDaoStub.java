package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_CREATED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_NAME_CHANGED;

/**
 * Mock implementation of {@link CollectionHistoryDao} for use while actual DB is still being set up and for testing.
 */
public class CollectionHistoryDaoStub extends CollectionHistoryDao {

    private static final List<CollectionHistoryEvent> mockHistory = new ArrayList<>();

    static {
        mockHistory.add(new CollectionHistoryEvent()
                .collectionId("1234567890")
                .collectionName("mockCollectionOne")
                .user("Batman@JusticeLeague.com")
                .eventType(COLLECTION_CREATED));

        mockHistory.add(new CollectionHistoryEvent()
                .collectionId("1234567890")
                .user("Superman@JusticeLeague.com")
                .collectionName("mockCollectionTwo")
                .eventType(COLLECTION_NAME_CHANGED)
                .addEventMetaData("previousName", "mockCollectionOne"));

        mockHistory.add(new CollectionHistoryEvent()
                .collectionId("1234567890")
                .user("Flash@JusticeLeague.com")
                .collectionName("mockCollectionThree")
                .eventType(COLLECTION_NAME_CHANGED));
    }

    @Override
    public void saveCollectionHistoryEvent(CollectionHistoryEvent event) throws ZebedeeException {
        mockHistory.add(event);
    }

    @Override
    public void saveCollectionHistoryEvent(Collection collection, Session session,
                                           CollectionEventType collectionEventType, CollectionEventMetaData... metaValues)
            throws ZebedeeException {

    }

    @Override
    public void saveCollectionHistoryEvent(String collectionName, String collectionId, Session session,
                                           CollectionEventType collectionEventType, CollectionEventMetaData... metaValues)
            throws ZebedeeException {

    }

    @Override
    public List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException {
        return mockHistory.stream()
                .filter(item -> item.getCollectionId().contains(collectionId))
                .collect(Collectors.toList());
    }
}
