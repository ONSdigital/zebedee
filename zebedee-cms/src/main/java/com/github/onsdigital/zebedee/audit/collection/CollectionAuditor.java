package com.github.onsdigital.zebedee.audit.collection;

import com.github.onsdigital.zebedee.exceptions.CollectionAuditException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.collection.audit.CollectionAuditEvent;
import com.github.onsdigital.zebedee.model.collection.audit.CollectionAuditHistory;
import com.github.onsdigital.zebedee.util.hibernate.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by dave on 5/26/16.
 */
public class CollectionAuditor {

    private static final CollectionAuditor AUDITOR = new CollectionAuditor();

    public static CollectionAuditor getAuditor() {
        return AUDITOR;
    }

    private CollectionAuditor() {
        // Hide constructor and force use of singleton instance.
    }

    public void save(CollectionAuditEvent event) throws ZebedeeException {
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

    public CollectionAuditHistory listAll() throws ZebedeeException {
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            List eventList = session.createSQLQuery("SELECT * FROM collection_audit").addEntity(CollectionAuditEvent.class).list();
            session.getTransaction().commit();

            CollectionAuditHistory results = new CollectionAuditHistory();
            eventList.stream().forEach(event -> results.add((CollectionAuditEvent)event));
            return results;

        } catch (Exception e) {
            logError(e, "Unexpected error while attempting to save collection audit event")
                    .logAndThrow(CollectionAuditException.class);
        }
        return null;
    }
}
