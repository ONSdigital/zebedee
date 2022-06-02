package com.github.onsdigital.zebedee.json;

/**
 * Represents a dataset that has been added to a collection.
 */
public class CollectionInteractive {

    private String id;
    private String title;
    private ContentStatus state;
    private String uri;
    private String lastEditedBy;

    private CollectionInteractiveFile[] files;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ContentStatus getState() {
        return state;
    }

    public void setState(ContentStatus state) {
        this.state = state;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setLastEditedBy(String user) {
        this.lastEditedBy = user;
    }

    public String getLastEditedBy() {
        return lastEditedBy;
    }

    public void setFiles(CollectionInteractiveFile[] files) { this.files = files; }

    public CollectionInteractiveFile[] getFiles() { return files; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectionInteractive that = (CollectionInteractive) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
