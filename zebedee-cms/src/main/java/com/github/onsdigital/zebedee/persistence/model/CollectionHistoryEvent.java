package com.github.onsdigital.zebedee.persistence.model;

import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.collection.audit.actions.CollectionEventType;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity object for a Collection audit history event.
 */
@Entity
@Table(name = "collection_history")
public class CollectionHistoryEvent {

    @Id
    @GeneratedValue
    @Column(name = "collection_history_event_id")
    private long id;

    @Column(name = "collection_id")
    private String collectionId;

    @Column(name = "event_date")
    private Date eventDate;

    @Column(name = "florence_user")
    private String user;

    @Column(name = "event_type")
    private CollectionEventType eventType;

    @Column(name = "collection_name")
    private String collectionName;

    @Column(name = "page_uri")
    private String pageURI;

    @Column(name = "file_uri")
    private String fileURI;

    @Column(name = "exception_text")
    private String exceptionText;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "event")
    private List<CollectionHistoryEventMetaData> collectionHistoryEventMetaData;

    public CollectionHistoryEvent() {
        this.eventDate = new Date();
    }

    public CollectionHistoryEvent(Collection collection, Session session, CollectionEventType collectionEventType) {
        this.eventDate = new Date();
        collectionId(collection.description.id);
        collectionName(collection.description.name);
        user(session.email);
        eventType(collectionEventType);
    }

    public long getId() {
        return id;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getUser() {
        return user;
    }

    public CollectionEventType getEventType() {
        return eventType;
    }

    public String getPageURI() {
        return pageURI;
    }

    public String getFileURI() {
        return fileURI;
    }

    public String getExceptionText() {
        return exceptionText;
    }

    public List<CollectionHistoryEventMetaData> getCollectionHistoryEventMetaData() {
        return collectionHistoryEventMetaData;
    }

    public CollectionHistoryEvent setId(long id) {
        this.id = id;
        return this;
    }

    public CollectionHistoryEvent collectionId(String collectionId) {
        this.collectionId = collectionId;
        return this;
    }

    public CollectionHistoryEvent eventDate(Date eventDate) {
        this.eventDate = eventDate;
        return this;
    }

    public CollectionHistoryEvent user(String user) {
        this.user = user;
        return this;
    }

    public CollectionHistoryEvent eventType(CollectionEventType eventType) {
        this.eventType = eventType;
        return this;
    }

    public CollectionHistoryEvent collectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }

    public CollectionHistoryEvent pageURI(String pageURI) {
        this.pageURI = pageURI;
        return this;
    }

    public CollectionHistoryEvent fileURI(String fileURI) {
        this.fileURI = fileURI;
        return this;
    }

    public CollectionHistoryEvent exceptionText(String exceptionText) {
        this.exceptionText = exceptionText;
        return this;
    }

    public CollectionHistoryEvent addEventMetaData(String name, String value) {
        if (this.collectionHistoryEventMetaData == null) {
            this.collectionHistoryEventMetaData = new ArrayList<>();
        }
        this.collectionHistoryEventMetaData.add(new CollectionHistoryEventMetaData(name, value, this));
        return this;
    }

    public CollectionHistoryEvent collectionEventMetaData(List<CollectionHistoryEventMetaData> collectionHistoryEventMetaData) {
        this.collectionHistoryEventMetaData = collectionHistoryEventMetaData;
        return this;
    }
}

