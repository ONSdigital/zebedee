package com.github.onsdigital.zebedee.model.collection.audit;

import com.github.onsdigital.zebedee.model.collection.audit.actions.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEventMetaData;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by dave on 6/2/16.
 */
public class CollectionHistoryEvent {

    private String collectionId;
    private Date eventDate;
    private String user;
    private CollectionEventType eventType;
    private String collectionName;
    private String pageURI;
    private String fileURI;
    private String exceptionText;
    private Map<String, String> metaData = null;

    public CollectionHistoryEvent(com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent event) {
        this.collectionId = event.getCollectionId();
        this.eventDate = event.getEventDate();
        this.user = event.getUser();
        this.eventType = event.getEventType();
        this.collectionName = event.getCollectionName();
        this.pageURI = event.getPageURI();
        this.fileURI = event.getFileURI();
        this.exceptionText = event.getExceptionText();

        if (event.getCollectionHistoryEventMetaData() != null && !event.getCollectionHistoryEventMetaData().isEmpty()) {
            this.metaData = new HashMap<>();
            this.metaData = event.getCollectionHistoryEventMetaData()
                    .stream()
                    .collect(Collectors.toMap(
                            CollectionHistoryEventMetaData::getKey, CollectionHistoryEventMetaData::getValue)
                    );
        }
    }

    public CollectionHistoryEvent() {
        this.metaData = new HashMap<>();
    }

    public String getCollectionId() {
        return collectionId;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public String getUser() {
        return user;
    }

    public CollectionEventType getEventType() {
        return eventType;
    }

    public String getCollectionName() {
        return collectionName;
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

    public Map<String, String> getMetaData() {
        return metaData;
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

    public CollectionHistoryEvent addMetaData(String name, String value) {
        this.metaData.put(name, value);
        return this;
    }
}
