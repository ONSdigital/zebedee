package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

/**
 * I dont care for all this collection stuff, I just want a CollectionWriter instance.
 */
public class DummyCollectionWriter extends CollectionWriter {

    private Path collections;

    public DummyCollectionWriter(Path collectionsPath) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        inProgress = getContentWriter(collectionsPath, getConfiguration().getInProgressFolderName());
        complete = getContentWriter(collectionsPath, getConfiguration().getCompleteFolderName());
        reviewed = getContentWriter(collectionsPath, getConfiguration().getReviewedFolderName());
    }

    private ContentWriter getContentWriter(Path collectionPath, String folderName) throws IOException {

        if (!Files.exists(collectionPath.resolve(folderName))) {
            Files.createDirectory(collectionPath.resolve(folderName));
        }

        return new ContentWriter(collectionPath.resolve(folderName));
    }

}
