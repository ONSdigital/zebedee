package com.github.onsdigital.zebedee.model.collection.audit;

import com.github.onsdigital.zebedee.model.collection.audit.actions.AuditAction;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Map;

/**
 * POJO representing a collection audit event.
 */
public class CollectionAuditEvent {

    private long id;
    private String collectionId;
    private Date eventDate;
    private String user;
    private AuditAction eventType;

    private String collectionName;
    private String pageURI;
    private String fileURI;
    private String exceptionText;
    private Map<String, String> metaData;

    public CollectionAuditEvent() {
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

    public AuditAction getEventType() {
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

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setEventType(AuditAction eventType) {
        this.eventType = eventType;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setPageURI(String pageURI) {
        this.pageURI = pageURI;
    }

    public void setFileURI(String fileURI) {
        this.fileURI = fileURI;
    }

    public void setExceptionText(String exceptionText) {
        this.exceptionText = exceptionText;
    }

    public void setMetaData(Map<String, String> metaData) {
        this.metaData = metaData;
    }

    public CollectionAuditEvent(Date eventDate, String collectionId, String user, AuditAction eventType, String collectionName,
                                String pageURI, String fileURI, String exceptionText, Map<String, String> metaData) {
        // Mandatory fields.
        this.eventDate = eventDate;
        this.collectionId = collectionId;
        this.user = user;
        this.eventType = eventType;

        // Optional fields.
        this.collectionName = StringUtils.isNotEmpty(collectionName) ? collectionName : null;
        this.pageURI = StringUtils.isNotEmpty(pageURI) ? pageURI : null;
        this.fileURI = StringUtils.isNotEmpty(fileURI) ? fileURI : null;
        this.exceptionText = StringUtils.isNotEmpty(exceptionText) ? exceptionText : null;
        this.metaData = metaData == null || metaData.isEmpty() ? null : metaData;
    }

    @Override
    public String toString() {
        return "CollectionAuditEvent{" +
                "id: " + id +
                ", collectionId:'" + collectionId + '\'' +
                ", eventDate: " + eventDate +
                ", user: '" + user + '\'' +
                ", eventType: " + eventType +
                ", collectionName: '" + collectionName + '\'' +
                ", pageURI: '" + pageURI + '\'' +
                ", fileURI: '" + fileURI + '\'' +
                ", exceptionText: '" + exceptionText + '\'' +
                ", metaData: " + metaData
                + '}';
    }
}

