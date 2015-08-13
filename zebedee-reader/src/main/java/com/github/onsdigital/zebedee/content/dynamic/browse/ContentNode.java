package com.github.onsdigital.zebedee.content.dynamic.browse;

import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;

/**
 * Created by bren on 03/08/15.
 * Represents a node in content hierarchy
 * <p>
 * Default sorting is by title alphabetically
 */
public class ContentNode implements Comparable<ContentNode> {

    private URI uri;
    private ContentNodeDetails description;
    private PageType type;

    private Collection<ContentNode> children;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public PageType getType() {
        return type;
    }

    public void setType(PageType type) {
        this.type = type;
    }

    public ContentNodeDetails getDescription() {
        return description;
    }

    public void setDescription(ContentNodeDetails description) {
        this.description = description;
    }

    public Collection<ContentNode> getChildren() {
        return children;
    }

    public void setChildren(Collection<ContentNode> children) {
        this.children = children;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.uri == null) {
            return super.equals(obj);
        }
        return this.uri.equals(obj);
    }

    @Override
    public int hashCode() {
        if (this.uri == null) {
            return super.hashCode();
        }
        return this.uri.hashCode();
    }

    @Override
    public int compareTo(ContentNode o) {
        if (isNull(getDescription())) {
            return 1;//Empty titles are listed as last elements
        }
        if (isNull(o) || isNull(o.getDescription())) {
            return -1;
        }

        int result = compare(getDescription().getTitle(), o.getDescription().getTitle());
        //compare editions if titles are the same
        if (result == 0) {
            result = compare(getDescription().getEdition(), o.getDescription().getEdition());
        }
        return result;
    }

    private int compare(String s1, String s2) {
        if (isNull(s1)) {
            return 1;//nulls last
        }
        if (isNull(s2)) {
            return -1;
        }

        return s1.compareTo(s2);
    }

    private boolean isNull(Object o) {
        return o == null;
    }


    public class ContentNodeComparator implements Comparator<ContentNode> {

        @Override
        public int compare(ContentNode o1, ContentNode o2) {
            return 0;
        }
    }

}
