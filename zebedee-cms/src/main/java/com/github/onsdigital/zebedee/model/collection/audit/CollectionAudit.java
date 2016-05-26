package com.github.onsdigital.zebedee.model.collection.audit;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * POJO representing a collection audit event.
 */
public class CollectionAudit {

    private Date eventDate;
    private String collectionId;
    private String user;
    private AuditAction eventType;
    private String pageURI;
    private String fileURI;
    private String exceptionText;

    private CollectionAudit(Date eventDate, String collectionId, String user, AuditAction eventType, String pageURI,
                            String fileURI, String exceptionText){
        this.eventDate = eventDate;
        this.collectionId = collectionId;
        this.user = user;
        this.eventType = eventType;

        this.pageURI = StringUtils.isNotEmpty(pageURI) ? pageURI : null;
        this.fileURI = StringUtils.isNotEmpty(fileURI) ? fileURI : null;
        this.exceptionText = StringUtils.isNotEmpty(exceptionText) ? exceptionText : null;
    }

    /**
     * Provides a convenient api for creating a {@link CollectionAudit}'s.
     */
    public static class Builder {

        private String collectionId;
        private String user;
        private AuditAction eventType;
        private String pageURI;
        private String fileURI;
        private String exceptionText;

        public Builder collectionId(String collectionId) {
            this.collectionId = collectionId;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder eventAction(AuditAction eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder pageURI(String pageURI) {
            this.pageURI = pageURI;
            return this;
        }

        public Builder fileURI(String fileURI) {
            this.fileURI = fileURI;
            return this;
        }

        public Builder exceptionText(String exceptionText) {
            this.exceptionText = exceptionText;
            return this;
        }

        public CollectionAudit build() {
            return new CollectionAudit(new Date(), collectionId, user, eventType, pageURI, fileURI, exceptionText);
        }
    }
}

