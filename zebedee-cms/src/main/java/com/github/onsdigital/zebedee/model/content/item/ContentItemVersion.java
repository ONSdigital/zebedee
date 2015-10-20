package com.github.onsdigital.zebedee.model.content.item;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;

import java.net.URI;
import java.nio.file.Path;

/**
 * Represents a single version of a content item.
 * <p>
 * A versioned content item lives under a "previous" directory which lives alongside the current version.
 */
public class ContentItemVersion extends ContentItem {

    private final VersionedContentItem versionedContentItem; // the parent content item
    private final String identifier;

    /**
     * Create a new instance of ContentItemVersion
     *
     * @param identifier
     * @param path                 - The path when the content item exists
     * @param versionedContentItem - the parent VersionedContentItem that the version exists under.
     * @param uri
     */
    public ContentItemVersion(String identifier, Path path, VersionedContentItem versionedContentItem, URI uri) throws NotFoundException {
        super(uri, path);
        this.identifier = identifier;
        this.versionedContentItem = versionedContentItem;
    }

    public VersionedContentItem getVersionedContentItem() {
        return versionedContentItem;
    }

    public String getIdentifier() {
        return identifier;
    }
}
