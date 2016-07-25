package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.json.DeleteMarkerJson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Created by dave on 7/25/16.
 */
public class DeleteMarker {

    static final String DATE_JSON_EXT = "/data.json";

    private String uri;
    private String title;
    private String user;
    private String collectionId;

    public String getUri() {
        return uri;
    }

    public DeleteMarker setUri(String uri) {
        if (!uri.toLowerCase().endsWith(DATE_JSON_EXT)) {
            uri = uri + DATE_JSON_EXT;
        }
        this.uri = uri;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public DeleteMarker setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getUser() {
        return user;
    }

    public DeleteMarker setUser(String user) {
        this.user = user;
        return this;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public DeleteMarker setCollectionId(String collectionId) {
        this.collectionId = collectionId;
        return this;
    }

    public static DeleteMarker jsonToMarker(DeleteMarkerJson json) {
        return new DeleteMarker()
                .setUri(json.getUri())
                .setTitle(json.getTitle())
                .setCollectionId(json.getCollectionId())
                .setUser(json.getUser());
    }

    public static DeleteMarkerJson markerToJson(DeleteMarker marker) {
        return new DeleteMarkerJson()
                .setUri(marker.getUri())
                .setTitle(marker.getTitle())
                .setCollectionId(marker.getCollectionId())
                .setUser(marker.getUser());
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
}
