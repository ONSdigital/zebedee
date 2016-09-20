package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.nio.file.Paths;

/**
 * POJO encapsulating details required when marking a content item to be deleted.
 */
public class DeleteMarkerJson {

    static final String DATE_JSON_EXT = "/data.json";

    private String uri;
    private String title;
    private String user;
    private String collectionId;
    private PageType type;

    public String getUri() {
        return uri;
    }

    public String getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public PageType getType() {
        return type;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public DeleteMarkerJson setUri(String uri) {
        if (uri.toLowerCase().endsWith(DATE_JSON_EXT)) {
            uri = Paths.get(uri).getParent().toString();
        }
        this.uri = uri;
        return this;
    }

    public DeleteMarkerJson setCollectionId(String collectionId) {
        this.collectionId = collectionId;
        return this;
    }

    public DeleteMarkerJson setTitle(String title) {
        this.title = title;
        return this;
    }

    public DeleteMarkerJson setUser(String user) {
        this.user = user;
        return this;
    }

    public DeleteMarkerJson setType(PageType type) {
        this.type = type;
        return this;
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
