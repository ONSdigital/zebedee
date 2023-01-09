package com.github.onsdigital.zebedee.json;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.github.onsdigital.zebedee.content.page.base.PageType;

/**
 * Class to hold a file uri and any other properties required from that file.
 */
public class ContentDetail {
    public String uri;
    private PageType type;
    public ContentDetailDescription description;

    public List<ContentDetail> children;
    public List<Event> events;
    public String contentPath;

    public ContentDetail() {
    }

    public ContentDetail(String uri, PageType type) {
        this.uri = uri;
        this.type = type;
    }

    public ContentDetail(String title, String uri, PageType type) {
        this(new ContentDetailDescription(title), uri, type);
    }

    public ContentDetail(ContentDetailDescription description, String uri, PageType type) {
        this(uri, type);
        this.description = description;
    }

    public ContentDetail(ContentDetailDescription description, String uri, PageType type, String contentPath) {
        this(description, uri, type);
        this.contentPath = contentPath;
    }

    /**
     * Creates a deep copy of this content detail instance, including child items.
     *
     * @return
     */
    public ContentDetail clone() {
        ContentDetail cloned = new ContentDetail(this.description, this.uri, this.type);
        cloned.contentPath = this.contentPath;

        if (this.children != null) {
            cloned.children = new ArrayList<>(this.children.size());
            this.children.forEach(child -> cloned.children.add(child.clone()));
        }

        return cloned;
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

    /**
     * Return the child item with the given uri
     *
     * @param uri
     * @return
     */
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

    /**
     * Return the child item with the given name.
     *
     * @param name
     * @return
     */
    public ContentDetail getChildWithName(String name) {

        if (this.children == null)
            return null;

        for (ContentDetail child : this.children) {
            if (child.description.title.equals(name)) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContentDetail that = (ContentDetail) o;
        return !(uri != null ? !uri.equals(that.uri) : that.uri != null);
    }

    @Override
    public int hashCode() {
        return uri != null ? uri.hashCode() : 0;
    }

    /**
     * Overlay the given list of ContentDetail items nested into this ContentDetail instance.
     *
     * @param toOverlay
     * @return
     */
    public ContentDetail overlayDetails(Iterable<ContentDetail> toOverlay) {

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
            // see if the content detail instance already exists at this level
            ContentDetail child = this.getChildWithUri("/" + path.subpath(0, depth + 1).toString());

            // if it doesn't exists try and resolve it using the folder name
            if (child == null)
                child = this.getChildWithName(path.subpath(depth, depth + 1).toString());

            // if child is still null then its a directory and needs creating and added as a child
            if (child == null) {
                String directoryName = path.subpath(depth, depth + 1).toString();

                // Create the new content
                child = new ContentDetail(directoryName, "", null);


                if (this.children == null) {
                    this.children = new ArrayList<>();
                }
                this.children.add(child);
            }

            if (child != null)
                child.overlayContentDetail(contentDetail, depth + 1);
        }
    }

    public PageType getType() {
        return type;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public ContentDetailDescription getDescription() {
        return description;
    }

    public void setDescription(ContentDetailDescription description) {
        this.description = description;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}

