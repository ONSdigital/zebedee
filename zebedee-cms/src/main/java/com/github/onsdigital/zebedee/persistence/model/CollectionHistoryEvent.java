package com.github.onsdigital.zebedee.persistence.model;

import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity object for a Collection event history.
 */
@Entity
@Table(name = "collection_history")
public class CollectionHistoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "collection_history_seq")
    @SequenceGenerator(name = "collection_history_seq",
            sequenceName = "collection_history_collection_history_event_id_seq",
            allocationSize = 1)
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

    public CollectionHistoryEvent(Collection collection, Session session, CollectionEventType collectionEventType,
                                  CollectionEventMetaData... metaValues) {
        this(collection.description.id, collection.description.name, session, collectionEventType, metaValues);
    }

    public CollectionHistoryEvent(String collectionId, String collectionName, Session session,
                                  CollectionEventType collectionEventType, CollectionEventMetaData... metaValues) {
        this.eventDate = new Date();
        collectionId(collectionId);
        collectionName(collectionName);
        user(session.email);
        eventType(collectionEventType);

        for (CollectionEventMetaData collectionEventMetaData : metaValues) {
            addEventMetaData(collectionEventMetaData.getKey(), collectionEventMetaData.getValue());
        }
    }

    public long getId() {
        return id;
    }

    public CollectionHistoryEvent setId(long id) {
        this.id = id;
        return this;
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

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, new String[] {"eventDate"});
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}

