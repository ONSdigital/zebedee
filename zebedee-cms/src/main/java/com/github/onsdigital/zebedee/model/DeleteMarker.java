package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.json.DeleteMarkerJson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by dave on 7/25/16.
 */
public class DeleteMarker {

    static final String DATE_JSON_EXT = "/data.json";

    private String uri;
    private String title;
    private String user;
    private String collectionId;
    private PageType type;

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public String getUser() {
        return user;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public PageType getType() {
        return type;
    }

    public DeleteMarker setUri(String uri) {
        if (!uri.toLowerCase().endsWith(DATE_JSON_EXT)) {
            uri = uri + DATE_JSON_EXT;
        }
        this.uri = uri;
        return this;
    }

    public DeleteMarker setTitle(String title) {
        this.title = title;
        return this;
    }

    public DeleteMarker setUser(String user) {
        this.user = user;
        return this;
    }

    public DeleteMarker setCollectionId(String collectionId) {
        this.collectionId = collectionId;
        return this;
    }

    public DeleteMarker setType(PageType type) {
        this.type = type;
        return this;
    }

    public Path getPath() {
        return Paths.get(this.getUri());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public static DeleteMarker jsonToMarker(DeleteMarkerJson json) {
        return new DeleteMarker()
                .setUri(json.getUri())
                .setTitle(json.getTitle())
                .setCollectionId(json.getCollectionId())
                .setUser(json.getUser())
                .setType(json.getType());
    }

    public static DeleteMarkerJson markerToJson(DeleteMarker marker) {
        return new DeleteMarkerJson()
                .setUri(marker.getUri())
                .setTitle(marker.getTitle())
                .setCollectionId(marker.getCollectionId())
                .setUser(marker.getUser())
                .setType(marker.getType());
    }
}
