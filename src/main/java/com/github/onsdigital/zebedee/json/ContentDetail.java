package com.github.onsdigital.zebedee.json;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold a file uri and any other properties required from that file.
 */
public class ContentDetail {
    public String uri;
    public String title;
    public String type;

    public List<ContentDetail> children;

    public ContentDetail() {
    }

    /**
     * Convenience constructor taking the typical parameters.
     *
     * @param title
     * @param uri
     * @param type
     */
    public ContentDetail(String title, String uri, String type) {
        this.uri = uri;
        this.title = title;
        this.type = type;
    }


    /**
     * Return true if this content contains the given child item.
     *
     * @param child
     * @return
     */
    public boolean containsChild(ContentDetail child) {

        if (this.children == null)
            return false;

        return this.children.contains(child);
    }

    public ContentDetail getChildWithUri(String uri) {

        if (this.children == null)
            return null;

        for (ContentDetail child : this.children) {
            if (child.uri.equals(uri)) {
                return child;
            }
        }

        return null;
    }

    public ContentDetail getChildWithName(String name) {

        if (this.children == null)
            return null;

        for (ContentDetail child : this.children) {
            if (child.title.equals(name)) {
                return child;
            }
        }

        return null;
    }

    /**
     * Return true if this content contents the given item as a descendant.
     *
     * @param descendant
     * @return
     */
    public boolean containsDescendant(ContentDetail descendant) {
        if (this.children != null) {
            if (this.containsChild(descendant)) {
                return true;
            }

            for (ContentDetail child : children) {
                if (child.containsDescendant(descendant)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * If the type, title, and uri are the same they are considered equal.
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContentDetail that = (ContentDetail) o;

        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public ContentDetail overlayDetails(List<ContentDetail> toOverlay) {

        for (ContentDetail contentDetail : toOverlay) {
            if (!this.containsDescendant(contentDetail)) {
                overlayContentDetail(contentDetail, 0);
            }
        }

        return this;
    }

    private void overlayContentDetail(ContentDetail contentDetail, int depth) {
        Path path = Paths.get(contentDetail.uri.replaceFirst("/", ""));

        if (path.subpath(depth, path.getNameCount()).getNameCount() < 2) {
            if (this.children == null)
                this.children = new ArrayList<>();
            this.children.add(contentDetail);
        } else {
            // recurse
            ContentDetail child = this.getChildWithUri("/" + path.subpath(0, depth + 1).toString());

            if (child == null)
                child = this.getChildWithName(path.subpath(depth, depth + 1).toString());

            if (child != null) {
                child.overlayContentDetail(contentDetail, depth + 1);
            }
        }

    }
}
