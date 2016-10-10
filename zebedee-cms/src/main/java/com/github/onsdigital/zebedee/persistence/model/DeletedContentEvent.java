package com.github.onsdigital.zebedee.persistence.model;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Entity object for a deleted content event.
 */
@Entity
@Table(name = "deleted_content")
public class DeletedContentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deleted_content_sequence_generator")
    @SequenceGenerator(name = "deleted_content_sequence_generator",
            sequenceName = "deleted_content_id_sequence",
            allocationSize = 1)
    @Column(name = "deleted_content_event_id")
    private long id;

    @Column(name = "collection_id", nullable = false)
    private String collectionId;

    @Column(name = "collection_name", nullable = false)
    private String collectionName;

    @Column(name = "event_date", nullable = false)
    private Date eventDate;

    @Column(name = "uri")
    private String uri;

    @Column(name = "page_title")
    private String pageTitle;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "deletedContentEvent")
    private List<DeletedFile> deletedFiles;

    public DeletedContentEvent() { }

    public DeletedContentEvent(String collectionId, String collectionName, Date eventDate, String uri, String pageTitle) {
        this.collectionId = collectionId;
        this.collectionName = collectionName;
        this.eventDate = eventDate;
        this.uri = uri;
        this.pageTitle = pageTitle;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public List<DeletedFile> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(List<DeletedFile> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}

