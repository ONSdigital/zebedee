package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.get;

/**
 * I dont care for all this collection stuff, I just want a CollectionWriter instance.
 */
public class DummyCollectionWriter extends CollectionWriter {

    private Path collections;

    public DummyCollectionWriter(Path collectionsPath, ReaderConfiguration configuration) throws BadRequestException,
            IOException, UnauthorizedException, NotFoundException {
        inProgress = getContentWriter(collectionsPath, configuration.getInProgressFolderName());
        complete = getContentWriter(collectionsPath, configuration.getCompleteFolderName());
        reviewed = getContentWriter(collectionsPath, configuration.getReviewedFolderName());
    }

    public DummyCollectionWriter(Path collectionsPath) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        inProgress = getContentWriter(collectionsPath, get().getInProgressFolderName());
        complete = getContentWriter(collectionsPath, get().getCompleteFolderName());
        reviewed = getContentWriter(collectionsPath, get().getReviewedFolderName());
    }

    private ContentWriter getContentWriter(Path collectionPath, String folderName) throws IOException {

        if (!Files.exists(collectionPath.resolve(folderName))) {
            Files.createDirectory(collectionPath.resolve(folderName));
        }

        return new ContentWriter(collectionPath.resolve(folderName));
    }

}
