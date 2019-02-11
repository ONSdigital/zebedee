package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEventMetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Mock implementation of {@link CollectionHistoryDao} for use while actual DB is still being set up and for testing.
 */
public class CollectionHistoryDaoStub implements CollectionHistoryDao {

    private static final List<CollectionHistoryEvent> emptyList = new ArrayList<>();

    @Override
    public Future saveCollectionHistoryEvent(CollectionHistoryEvent event) {
        info().data("collectionId", event.getCollectionId())
                .data("collectionName", event.getCollectionName())
                .data("eventDate", event.getEventDate())
                .data("user", event.getUser())
                .data("eventType", event.getEventType())
                .data("uri", event.getUri())
                .data("exceptionText", event.getExceptionText())
                .data("metaData", event.getCollectionHistoryEventMetaData().stream().collect(Collectors.toMap
                        (CollectionHistoryEventMetaData::getKey, CollectionHistoryEventMetaData::getValue)))
                .log(event.getEventType().name());
        return new DummyFuture();
    }

    @Override
    public Future saveCollectionHistoryEvent(Collection collection, Session session,
                                             CollectionEventType collectionEventType, CollectionEventMetaData... metaValues) {
        this.saveCollectionHistoryEvent(new CollectionHistoryEvent(collection, session, collectionEventType, metaValues));
        return new DummyFuture();
    }

    @Override
    public Future saveCollectionHistoryEvent(String collectionName, String collectionId, Session session,
                                           CollectionEventType collectionEventType, CollectionEventMetaData... metaValues) {
        this.saveCollectionHistoryEvent(new CollectionHistoryEvent(collectionId, collectionName, session, collectionEventType,
                metaValues));
        return new DummyFuture();
    }

    @Override
    public List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException {
        info().log("getCollectionEventHistory: AUDIT database is not enabled Events are written to application log only.");
        return emptyList;
    }

    private static class DummyFuture implements Future {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
