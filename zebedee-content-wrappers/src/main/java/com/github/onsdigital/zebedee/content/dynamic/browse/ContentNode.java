package com.github.onsdigital.zebedee.content.dynamic.browse;

import com.github.onsdigital.zebedee.content.dynamic.TitleWrapper;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Created by bren on 03/08/15.
 * Represents a node in content hierarchy
 *
 * Default sorting is by title alphabetically
 */
public class ContentNode implements Comparable<ContentNode> {

    private URI uri;
    private TitleWrapper description;
    private PageType type;

    private Collection<ContentNode> children;

    public ContentNode(URI uri, String title, PageType type) {
        this.uri = uri;
        this.description = new TitleWrapper(title);
        this.type = type;
    }

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

    public TitleWrapper getDescription() {
        return description;
    }

    public void setDescription(TitleWrapper description) {
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
        if(isNull(getDescription()) || isNull(getDescription().getTitle())) {
            return 1;//Empty titles are listed as last elements
        }
        if(isNull(o) || isNull(o.getDescription()) || isNull(o.getDescription().getTitle())){
            return -1;
        }

        return getDescription().getTitle().compareTo(o.getDescription().getTitle());
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
