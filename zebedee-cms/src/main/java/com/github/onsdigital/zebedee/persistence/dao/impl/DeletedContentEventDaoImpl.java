package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.onsdigital.zebedee.model.content.deleted.DeletedContentEvent;
import com.github.onsdigital.zebedee.model.content.deleted.DeletedFile;
import com.github.onsdigital.zebedee.persistence.HibernateService;
import com.github.onsdigital.zebedee.persistence.HibernateServiceImpl;
import com.github.onsdigital.zebedee.persistence.dao.DeletedContentEventDao;
import org.hibernate.Hibernate;
import org.hibernate.Session;

import java.util.ArrayList;
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

        com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent eventToSave = mapToDatabaseObject(deletedContentEvent);

        Session session = hibernateService.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        session.save(eventToSave);
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

        List<com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent> events = session
                .createSQLQuery("SELECT * FROM deleted_content ORDER BY event_date ASC LIMIT 50")
                .addEntity(com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent.class)
                .list();

        for (com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent event : events) {
            Hibernate.initialize(event.getDeletedFiles()); // the deleted files have lazy instantiation, so ensure they are in the result here
        }

        session.getTransaction().commit();

        List<DeletedContentEvent> deletedContentEvents = new ArrayList<>();
        for (com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent event : events) {
            DeletedContentEvent deletedContentEvent = mapToDto(event);
            deletedContentEvents.add(deletedContentEvent);
        }

        return deletedContentEvents;
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

        com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent event = (com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent) session
                .createSQLQuery("SELECT * FROM deleted_content WHERE deleted_content_event_id = :event_id ORDER BY event_date ASC LIMIT 50")
                .addEntity(com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent.class)
                .setLong("event_id", deletedContentEventId)
                .uniqueResult();

        Hibernate.initialize(event.getDeletedFiles()); // the deleted files have lazy instantiation, so ensure they are in the result here
        session.getTransaction().commit();

        DeletedContentEvent deletedContentEvent = mapToDto(event);

        return deletedContentEvent;
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

    private com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent mapToDatabaseObject(DeletedContentEvent deletedContentEvent) {

        com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent databaseRepresentation = new com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent(
                deletedContentEvent.getCollectionId(),
                deletedContentEvent.getCollectionName(),
                deletedContentEvent.getEventDate(),
                deletedContentEvent.getUri(),
                deletedContentEvent.getPageTitle());

        ArrayList<com.github.onsdigital.zebedee.persistence.model.DeletedFile> deletedFiles = new ArrayList<>();

        for (com.github.onsdigital.zebedee.model.content.deleted.DeletedFile deletedFile : deletedContentEvent.getDeletedFiles()) {
            deletedFiles.add(new com.github.onsdigital.zebedee.persistence.model.DeletedFile(deletedFile.getUri(), databaseRepresentation));
        }

        databaseRepresentation.setDeletedFiles(deletedFiles);

        return databaseRepresentation;
    }

    private DeletedContentEvent mapToDto(com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent deletedContentEvent) {

        DeletedContentEvent dto = new DeletedContentEvent(
                deletedContentEvent.getCollectionId(),
                deletedContentEvent.getCollectionName(),
                deletedContentEvent.getEventDate(),
                deletedContentEvent.getUri(),
                deletedContentEvent.getPageTitle());

        dto.setId(deletedContentEvent.getId());
        ArrayList<DeletedFile> deletedFiles = new ArrayList<>();

        for (com.github.onsdigital.zebedee.persistence.model.DeletedFile deletedFile : deletedContentEvent.getDeletedFiles()) {
            DeletedFile deletedFileDto = new DeletedFile(deletedFile.getUri());
            deletedFileDto.setId(deletedFile.getId());
            deletedFiles.add(deletedFileDto);
        }

        dto.setDeletedFiles(deletedFiles);

        return dto;
    }
}
