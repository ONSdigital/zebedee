package com.github.onsdigital.zebedee.json;

/**
 * Represents a particular version of a dataset within a collection.
 */
public class CollectionDatasetVersion {

    private String id;
    private String title;
    private String edition;
    private String version;
    private String uri;
    private ContentStatus state;
    private String lastEditedBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectionDatasetVersion that = (CollectionDatasetVersion) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getEdition() != null ? !getEdition().equals(that.getEdition()) : that.getEdition() != null) return false;
        return getVersion() != null ? getVersion().equals(that.getVersion()) : that.getVersion() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getEdition() != null ? getEdition().hashCode() : 0);
        result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
        return result;
    }
}
