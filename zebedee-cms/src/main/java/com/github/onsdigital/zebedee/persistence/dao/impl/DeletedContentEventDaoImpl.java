package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.onsdigital.zebedee.persistence.HibernateService;
import com.github.onsdigital.zebedee.persistence.HibernateServiceImpl;
import com.github.onsdigital.zebedee.persistence.dao.DeletedContentEventDao;
import com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent;
import com.github.onsdigital.zebedee.persistence.model.DeletedFile;
import org.hibernate.Hibernate;
import org.hibernate.Session;

import java.util.Date;
import java.util.List;

public class DeletedContentEventDaoImpl implements DeletedContentEventDao {

    private HibernateService hibernateService;

    /**
     * Constructor allowing injection of the HibernateService instance.
     *
     * @param hibernateService
     */
    public DeletedContentEventDaoImpl(HibernateService hibernateService) {
        this.hibernateService = hibernateService;
    }

    /**
     * Constructor with no arguments uses default dependency implementations.
     */
    public DeletedContentEventDaoImpl() {
        hibernateService = HibernateServiceImpl.getInstance();
    }

    /**
     * Add a new deleted content event to the database.
     *
     * @param deletedContentEvent
     */
    @Override
    public DeletedContentEvent saveDeletedContentEvent(DeletedContentEvent deletedContentEvent) {
        Session session = hibernateService.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        session.save(deletedContentEvent);
        session.getTransaction().commit();

        return deletedContentEvent;
    }

    /**
     * Return a list of deleted content events.
     *
     * @return
     */
    @Override
    public List<DeletedContentEvent> listDeletedContent() {
        Session session = hibernateService.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        List<DeletedContentEvent> events = session
                .createSQLQuery("SELECT * FROM deleted_content ORDER BY event_date ASC LIMIT 50")
                .addEntity(DeletedContentEvent.class)
                .list();

        for (DeletedContentEvent event : events) {
            Hibernate.initialize(event.getDeletedFiles()); // the deleted files have lazy instantiation, so ensure they are in the result here
        }

        session.getTransaction().commit();
        return events;
    }

    /**
     * Get the deleted content event and the files that were deleted.
     * @param deletedContentEventId
     * @return
     */
    @Override
    public DeletedContentEvent retrieveDeletedContent(long deletedContentEventId) {

        // query the db to determine all uris to be restored.
        Session session = hibernateService.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        DeletedContentEvent event = (DeletedContentEvent) session
                .createSQLQuery("SELECT * FROM deleted_content ORDER BY event_date ASC LIMIT 50")
                .addEntity(DeletedContentEvent.class)
                .uniqueResult();

        Hibernate.initialize(event.getDeletedFiles()); // the deleted files have lazy instantiation, so ensure they are in the result here
        session.getTransaction().commit();

        return event;
    }

    /**
     * Use for local testing.
     * @param args
     */
    public static void main(String[] args) {
        DeletedContentEventDaoImpl deletedContentEventDao = new DeletedContentEventDaoImpl();

        DeletedContentEvent event = new DeletedContentEvent("collectionid", "collectionNAme", new Date(), "/some/uri", "Page title");
        event.addDeletedFile("/some/uri/data.json");
        event.addDeletedFile("/some/uri/123.json");
        event.addDeletedFile("/some/uri/456.json");

        deletedContentEventDao.saveDeletedContentEvent(event);

        List<DeletedContentEvent> deletedContentEvents = deletedContentEventDao.listDeletedContent();

        for (DeletedContentEvent deletedContentEvent : deletedContentEvents) {
            System.out.println("deletedContentEvent.getUri() = " + deletedContentEvent.getUri());
            for (DeletedFile deletedFile : deletedContentEvent.getDeletedFiles()) {
                System.out.println(" - deletedFile = " + deletedFile.getUri());
            }
        }
    }
}
