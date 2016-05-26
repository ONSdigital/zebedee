package com.github.onsdigital.zebedee.model.collection.audit.builder;

import com.github.onsdigital.zebedee.audit.collection.CollectionAuditor;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.collection.audit.CollectionAuditEvent;
import com.github.onsdigital.zebedee.model.collection.audit.actions.AuditAction;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Created by dave on 5/26/16.
 */
public class CollectionAuditBuilder {

    private static final CollectionAuditor AUDITOR = CollectionAuditor.getAuditor();

    private String collectionId;
    private String collectionName;
    private String user;
    private AuditAction eventType;
    private String pageURI;
    private String fileURI;
    private String exceptionText;
    private Map<String, String> metaData;

    public static CollectionAuditBuilder getCollectionAuditBuilder() {
        return new CollectionAuditBuilder();
    }

    private CollectionAuditBuilder() {}


    public CollectionAuditBuilder collection(CollectionDescription description) {
        collectionId(description.id);
        this.collectionName = description.name;
        return this;
    }

    public CollectionAuditBuilder collection(Collection collection) {
        collectionId(collection.description.id);
        this.collectionName = collection.description.name;
        return this;
    }

    public CollectionAuditBuilder collectionId(String collectionId) {
        this.collectionId = collectionId;
        return this;
    }

    public CollectionAuditBuilder user(Session session) {
        return user(session.email);
    }

    public CollectionAuditBuilder user(String user) {
        this.user = user;
        return this;
    }

    public CollectionAuditBuilder eventAction(AuditAction eventType) {
        this.eventType = eventType;
        return this;
    }

    public CollectionAuditBuilder pageURI(String pageURI) {
        this.pageURI = pageURI;
        return this;
    }

    public CollectionAuditBuilder fileURI(String fileURI) {
        this.fileURI = fileURI;
        return this;
    }

    public CollectionAuditBuilder exceptionText(String exceptionText) {
        this.exceptionText = exceptionText;
        return this;
    }

    public CollectionAuditBuilder addMetaItem(String name, String value) {
        if (this.metaData == null) {
            this.metaData = new HashMap<>();
        }
        this.metaData.put(name, value);
        return this;
    }

    public CollectionAuditEvent build() {
        requireNonNull(collectionId, "collectionId is required and cannot be null");
        requireNonNull(collectionId, "user is required and cannot be null");
        requireNonNull(collectionId, "eventType is required and cannot be null");
        return new CollectionAuditEvent(new Date(), collectionId, user, eventType, collectionName,
                pageURI, fileURI, exceptionText, metaData);
    }

    public void save() throws ZebedeeException {
        AUDITOR.save(build());
    }
}
