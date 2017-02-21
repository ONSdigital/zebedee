package com.github.onsdigital.zebedee.model.content.deleted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by carlhembrough on 07/10/2016.
 */
public class DeletedContentEvent {
    private long id;
    private String collectionId;
    private String collectionName;
    private Date eventDate;
    private String uri;
    private String pageTitle;
    private List<DeletedFile> deletedFiles;

    public DeletedContentEvent(String collectionId, String collectionName, Date eventDate, String uri, String pageTitle) {
        this.collectionId = collectionId;
        this.collectionName = collectionName;
        this.eventDate = eventDate;
        this.uri = uri;
        this.pageTitle = pageTitle;
    }

    public DeletedContentEvent(String collectionId, String collectionName, Date eventDate, String uri, String pageTitle, Collection<String> deletedUrls) {
        this(collectionId, collectionName, eventDate, uri, pageTitle);
        deletedUrls.forEach(this::addDeletedFile);
    }

    public DeletedContentEvent() {
    }

    public void addDeletedFile(String uri) {
        if (deletedFiles == null) {
            deletedFiles = new ArrayList<>();
        }

        deletedFiles.add(new DeletedFile(uri));
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
}
