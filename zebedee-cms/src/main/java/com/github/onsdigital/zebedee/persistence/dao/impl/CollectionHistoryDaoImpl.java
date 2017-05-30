package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.onsdigital.zebedee.exceptions.CollectionEventHistoryException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.HibernateService;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import org.hibernate.Session;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Implementation of the {@link CollectionHistoryDao} interface. Provides methods for saving {@link CollectionHistoryEvent}'s
 * and getting events by collection ID.
 */
public class CollectionHistoryDaoImpl implements CollectionHistoryDao {

    private static ExecutorService threadPool;
    private static Function<ExecutorService, Thread> shutdownTask = (executorService ->
            new Thread(() -> executorService.shutdown()));

    static final String COLLECTION_ID = "collection_id";
    static final String SELECT_BY_COLLECTION_ID =
            "SELECT collection_history_event_id, collection_id, collection_name, " +
                    "event_date, event_type, exception_text, uri, florence_user" +
                    " FROM collection_history " +
                    "WHERE collection_id = :collection_id " +
                    "ORDER BY event_date ASC";
    private HibernateService hibernateService = null;

    static {
        threadPool = Executors.newFixedThreadPool(20);
        Runtime.getRuntime().addShutdownHook(shutdownTask.apply(threadPool));
    }

    public CollectionHistoryDaoImpl(HibernateService hibernateService) {
        this.hibernateService = hibernateService;
    }

    /**
     * Save a {@link CollectionHistoryEvent} to the database.
     *
     * @param event
     * @throws ZebedeeException
     */
    public Future saveCollectionHistoryEvent(CollectionHistoryEvent event) {
        return threadPool.submit(() -> {
            try {
                Session session = hibernateService.getSessionFactory().getCurrentSession();
                session.beginTransaction();
                session.save(event);
                session.getTransaction().commit();
            } catch (Exception ex) {
                logError(ex, "Unexpected error while attempting to save collection audit event")
                        .addParameter("event", event.toString()).throwUnchecked(ex);
            }
        });
    }

    @Override
    public Future saveCollectionHistoryEvent(Collection collection, com.github.onsdigital.zebedee.session.model.Session session,
                                             CollectionEventType collectionEventType, CollectionEventMetaData... metaValues) {
        return this.saveCollectionHistoryEvent(new CollectionHistoryEvent(collection.getDescription().getId(),
                collection.getDescription().getName(), session, collectionEventType, metaValues));
    }

    @Override
    public Future saveCollectionHistoryEvent(String collectionId, String collectionName,
                                             com.github.onsdigital.zebedee.session.model.Session session,
                                             CollectionEventType collectionEventType, CollectionEventMetaData... metaValues) {
        return this.saveCollectionHistoryEvent(new CollectionHistoryEvent(collectionId, collectionName, session,
                collectionEventType, metaValues));
    }

    public List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException {
        try {
            Session session = hibernateService.getSessionFactory().getCurrentSession();
            session.beginTransaction();

            List<CollectionHistoryEvent> events = (List<CollectionHistoryEvent>) session
                    .createSQLQuery(SELECT_BY_COLLECTION_ID)
                    .addEntity(CollectionHistoryEvent.class)
                    .setString(COLLECTION_ID, collectionId)
                    .list();

            session.getTransaction().commit();
            return events;

        } catch (Exception e) {
            logError(e, "Unexpected error while attempting to get collection event history")
                    .collectionId(collectionId)
                    .logAndThrow(CollectionEventHistoryException.class);
        }
        return null;
    }
}
