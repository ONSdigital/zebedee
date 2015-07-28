package com.github.onsdigital.zebedee.reader.model;

import java.net.URI;

/**
 * Created by bren on 28/07/15.
 * <p>
 * A node logically having other contents as child contents ( could be any other document or a folder if stored on a file system)
 */
public class ContentContainer extends Content {

    public ContentContainer(URI uri, String name) {
        super(uri, name);
    }

    /**
     * Used to determine if a content container is simply a container or a document with textual data
     */
    public boolean isDocument() {
        return false;
    }

}
