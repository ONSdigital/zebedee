package com.github.onsdigital.zebedee.persistence.model;

import com.github.onsdigital.zebedee.session.model.Session;
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

    @Column(name = "collection_id", nullable = false)
    private String collectionId;

    @Column(name = "collection_name", nullable = false)
    private String collectionName;

    @Column(name = "event_date", nullable = false)
    private Date eventDate;

    @Column(name = "florence_user", nullable = false)
    private String user;

    @Column(name = "event_type")
    private CollectionEventType eventType;

    @Column(name = "uri")
    private String uri;

    @Column(name = "exception_text")
    private String exceptionText;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "event")
    private List<CollectionHistoryEventMetaData> collectionHistoryEventMetaData;

    public CollectionHistoryEvent() {
        this.eventDate = new Date();
    }

    public CollectionHistoryEvent(Collection collection, Session session, CollectionEventType collectionEventType,
                                  String uri, CollectionEventMetaData... metaValues) {
        this(collection.getDescription().getId(), collection.getDescription().getName(), session, collectionEventType,
                metaValues);
        this.uri = uri;
    }

    public CollectionHistoryEvent(Collection collection, Session session, CollectionEventType collectionEventType,
                                  CollectionEventMetaData... metaValues) {
        this(collection.getDescription().getId(), collection.getDescription().getName(), session, collectionEventType,
                metaValues);
    }

    public CollectionHistoryEvent(String collectionId, String collectionName, Session session,
                                  CollectionEventType collectionEventType, CollectionEventMetaData... metaValues) {
        this.eventDate = new Date();
        collectionId(collectionId);
        collectionName(collectionName);
        eventType(collectionEventType);
        user(session.getEmail());

        if (metaValues != null) {
            for (CollectionEventMetaData collectionEventMetaData : metaValues) {
                addEventMetaData(collectionEventMetaData.getKey(), collectionEventMetaData.getValue());
            }
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

    public String getUri() {
        return uri;
    }

    public String getExceptionText() {
        return exceptionText;
    }

    public List<CollectionHistoryEventMetaData> getCollectionHistoryEventMetaData() {
        if (this.collectionHistoryEventMetaData == null) {
            this.collectionHistoryEventMetaData = new ArrayList<>();
        }
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

    public CollectionHistoryEvent uri(String uri) {
        this.uri = uri;
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
        return EqualsBuilder.reflectionEquals(this, obj, new String[]{"eventDate"});
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

