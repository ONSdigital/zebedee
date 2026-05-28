package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * I don't care for all this collection stuff, I just want a CollectionWriter instance.
 */
public class DummyCollectionReader extends CollectionReader {

    public DummyCollectionReader(Path collectionPath) throws IOException {
        if (collectionPath == null) {
            throw new NullPointerException("Collection path can not be null");
        }

        inProgress = getContentReader(collectionPath, Collection.IN_PROGRESS);
        complete = getContentReader(collectionPath, Collection.COMPLETE);
        reviewed = getContentReader(collectionPath, Collection.REVIEWED);
    }

    public DummyCollectionReader(Path collectionsRoot, String collectionId) throws IOException {
        this(collectionsRoot.resolve(collectionId.split("-")[0]));
    }

    private ContentReader getContentReader(Path collectionPath, String folderName) throws IOException {

        if (!Files.exists(collectionPath.resolve(folderName))) {
            Files.createDirectory(collectionPath.resolve(folderName));
        }

        return new FileSystemContentReader(collectionPath.resolve(folderName));
    }
}
