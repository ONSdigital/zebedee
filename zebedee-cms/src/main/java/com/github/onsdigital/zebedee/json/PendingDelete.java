package com.github.onsdigital.zebedee.json;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * POJO representing a pending deletion. Encapsulates the nodes of the browse tree affected by the delete and who
 * actioned in.
 */
public class PendingDelete {

    private String user;
    private ContentDetail root;

    public PendingDelete(String user, ContentDetail root) {
        this.user = user;
        this.root = root;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public ContentDetail getRoot() {
        return root;
    }

    public void setRoot(ContentDetail root) {
        this.root = root;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);

    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
