package com.github.onsdigital.zebedee.content.dynamic.browse;

import com.github.onsdigital.zebedee.content.dynamic.TitleWrapper;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.net.URI;
import java.util.List;

/**
 * Created by bren on 03/08/15.
 * Represents a node in content hierarchy
 */
public class ContentNode {

    private URI uri;
    private TitleWrapper description;
    private PageType type;

    private List<ContentNode> children;

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

    public List<ContentNode> getChildren() {
        return children;
    }

    public void setChildren(List<ContentNode> children) {
        this.children = children;
    }
}
