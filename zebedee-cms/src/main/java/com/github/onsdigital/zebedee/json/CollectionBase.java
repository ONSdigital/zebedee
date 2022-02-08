package com.github.onsdigital.zebedee.json;

import java.util.Date;
import java.util.List;

public class CollectionBase {
    /**
     * The unique identifier of this {@link com.github.onsdigital.zebedee.model.Collection}.
     */
    protected String id;
    /**
     * The readable name of this {@link com.github.onsdigital.zebedee.model.Collection}.
     */
    protected String name;
    /**
     * The type of the collection to determine the publish behaviour.
     */
    protected CollectionType type;
    /**
     * The date-time when this {@link com.github.onsdigital.zebedee.model.Collection} should be published (if it has
     * a publish date).
     */
    protected Date publishDate;
    /**
     * If the collection is associated with a release the releaseUri determines that release.
     */
    protected String releaseUri;

    @Deprecated
    protected List<String> teams; // list of team Id's

    protected List<String> viewerTeams; // list of team Id's

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CollectionType getType() {
        return type;
    }

    public void setType(CollectionType type) {
        this.type = type;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public String getReleaseUri() {
        return releaseUri;
    }

    public void setReleaseUri(String releaseUri) {
        this.releaseUri = releaseUri;
    }

    public List<String> getViewerTeams() {
        return viewerTeams;
    }

    public void setViewerTeams(List<String> viewerTeams) {
        this.viewerTeams = viewerTeams;
    }

    @Deprecated
    public List<String> getTeams() {
        return teams;
    }

    @Deprecated
    public void setTeams(List<String> teams) {
        this.teams = teams;
    }


}
