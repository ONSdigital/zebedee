package com.github.onsdigital.zebedee.model.publishing;

import java.nio.file.Path;
/**
 * Represents the store of published collections for adding to and searching.
 */
public class PublishedCollections {

    public final Path path;

    public PublishedCollections(Path path) {
        this.path = path;
    }
}
