package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.exceptions.CollectionAuditException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import com.github.onsdigital.zebedee.persistence.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by dave on 5/26/16.
 */
public class CollectionHistoryDaoImpl extends CollectionHistoryDao {

    private static final String COLLECTION_ID = "collection_id";

    private static final String BASE_SELECT = "SELECT collection_history_event_id, collection_id, collection_name, " +
            "event_date, event_type, exception_text, file_uri, page_uri, florence_user FROM collection_history";

    private static final String WHERE_NAME_LIKE = " WHERE collection_id = :" + COLLECTION_ID;

    private static final String ORDER_BY_DATE = " ORDER BY event_date ASC";

    private static final String QUERY_BY_COLLECTION_NAME = BASE_SELECT + WHERE_NAME_LIKE + ORDER_BY_DATE;

    CollectionHistoryDaoImpl() {
        // Hide constructor and force use of singleton instance.
    }

    /**
     * Save a {@link CollectionHistoryEvent} to the database.
     *
     * @param event
     * @throws ZebedeeException
     */
    public void saveCollectionHistoryEvent(CollectionHistoryEvent event) throws ZebedeeException {
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            session.save(event);
            session.getTransaction().commit();
        } catch (Exception e) {
            logError(e, "Unexpected error while attempting to save collection audit event")
                    .addParameter("event", event.toString())
                    .logAndThrow(CollectionAuditException.class);
        }
    }

    public List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException {
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();

            List<CollectionHistoryEvent> events = (List<CollectionHistoryEvent>) session
                    .createSQLQuery(QUERY_BY_COLLECTION_NAME)
                    .addEntity(CollectionHistoryEvent.class)
                    .setString(COLLECTION_ID, collectionId)
                    .list();

            session.getTransaction().commit();
            return events;

        } catch (Exception e) {
            logError(e, "Unexpected error while attempting to save collection audit event")
                    .logAndThrow(CollectionAuditException.class);
        }
        return null;
    }
}
