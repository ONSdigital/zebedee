package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.model.collection.audit.actions.CollectionEventType.COLLECTION_CREATED;
import static com.github.onsdigital.zebedee.model.collection.audit.actions.CollectionEventType.COLLECTION_EDIT_CHANGED_NAME;

/**
 * Mock implementation of {@link CollectionHistoryDao}.
 */
public class CollectionHistoryDaoStub extends CollectionHistoryDao {

    private static final List<CollectionHistoryEvent> mockHistory = new ArrayList<>();

    CollectionHistoryDaoStub() {
        // Hide constructor use static getInstance().
    }

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
                .eventType(COLLECTION_EDIT_CHANGED_NAME)
                .addEventMetaData("presviousName", "mockCollectionOne"));
    }

    @Override
    public void saveCollectionHistoryEvent(CollectionHistoryEvent event) throws ZebedeeException {
        mockHistory.add(event);
    }

    @Override
    public List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException {
        return mockHistory.stream()
                .filter(item -> item.getCollectionId().contains(collectionId))
                .collect(Collectors.toList());
    }
}
