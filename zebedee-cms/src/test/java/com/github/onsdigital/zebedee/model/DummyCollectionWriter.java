package com.github.onsdigital.zebedee.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * I don't care for all this collection stuff, I just want a CollectionWriter instance.
 */
public class DummyCollectionWriter extends CollectionWriter {

    public DummyCollectionWriter(Path collectionPath) throws IOException {
        if (collectionPath == null) {
            throw new NullPointerException("Collection path can not be null");
        }

        inProgress = getContentWriter(collectionPath, Collection.IN_PROGRESS);
        complete = getContentWriter(collectionPath, Collection.COMPLETE);
        reviewed = getContentWriter(collectionPath, Collection.REVIEWED);
    }

    public DummyCollectionWriter(Path collectionsRoot, String collectionId) throws IOException {
        this(collectionsRoot.resolve(collectionId.split("-")[0]));
    }

    private ContentWriter getContentWriter(Path collectionPath, String folderName) throws IOException {

        if (!Files.exists(collectionPath.resolve(folderName))) {
            Files.createDirectory(collectionPath.resolve(folderName));
        }

        return new ContentWriter(collectionPath.resolve(folderName));
    }

}
