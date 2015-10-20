package com.github.onsdigital.zebedee.model.content.item;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class to represent a single item of content.
 * <p>
 * This typically consists of a directory that contains a data.json file.
 * It can also contain any files associated with the page.
 */
public class ContentItem {

    private final URI uri;
    private final Path path;

    public ContentItem(URI uri, Path path) throws NotFoundException {
        this.uri = uri;
        this.path = path;

        if (!Files.exists(path)) {
            throw new NotFoundException(String.format("The give path for this content item does not exist: %s", path.toString()));
        }
    }

    public URI getUri() {
        return uri;
    }

    public Path getPath() {
        return path;
    }

    public Path getDataFilePath() {
        return path.resolve("data.json");
    }
}
