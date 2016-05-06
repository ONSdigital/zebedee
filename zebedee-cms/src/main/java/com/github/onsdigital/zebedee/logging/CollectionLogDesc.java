package com.github.onsdigital.zebedee.logging;

import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;
import java.util.List;

/**
 * POJO to hold log info about a {@link Collection}.
 */
public class CollectionLogDesc {

    private String path;
    private List<String> files;

    public CollectionLogDesc(Collection collection) throws IOException {
        this.path = collection.path.toString();
        this.files = collection.reviewedUris();
    }

    public String getPath() {
        return path;
    }

    public List<String> getFiles() {
        return files;
    }
}
