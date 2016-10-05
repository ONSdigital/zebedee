package com.github.onsdigital.zebedee.persistence.model;

import javax.persistence.*;
import java.util.ArrayList;
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

    @Column(name = "florence_user", nullable = false)
    private String user;

    @Column(name = "uri")
    private String uri;

    @Column(name = "page_title")
    private String pageTitle;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "deletedContentEvent")
    private List<DeletedFile> deletedFiles;

    public DeletedContentEvent(String collectionId, String collectionName, Date eventDate, String user, String uri, String pageTitle) {
        this.collectionId = collectionId;
        this.collectionName = collectionName;
        this.eventDate = eventDate;
        this.user = user;
        this.uri = uri;
        this.pageTitle = pageTitle;
    }

    /**
     * Default constructor required by hibernate.
     */
    public DeletedContentEvent() {
    }

    public void addDeletedFile(String uri) {
        if(deletedFiles == null) {
            deletedFiles = new ArrayList<>();
        }

        deletedFiles.add(new DeletedFile(uri, this));
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}

