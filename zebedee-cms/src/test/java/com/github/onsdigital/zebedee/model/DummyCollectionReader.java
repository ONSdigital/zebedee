package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.get;

/**
 * I dont care for all this collection stuff, I just want a CollectionWriter instance.
 */
public class DummyCollectionReader extends CollectionReader {

    private Path collections;

    public DummyCollectionReader(Path collectionsPath, ReaderConfiguration configuration) throws BadRequestException,
            IOException, UnauthorizedException, NotFoundException {
        inProgress = getContentReader(collectionsPath, configuration.getInProgressFolderName());
        complete = getContentReader(collectionsPath, configuration.getCompleteFolderName());
        reviewed = getContentReader(collectionsPath, configuration.getReviewedFolderName());
    }

    public DummyCollectionReader(Path collectionsPath) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        inProgress = getContentReader(collectionsPath, get().getInProgressFolderName());
        complete = getContentReader(collectionsPath, get().getCompleteFolderName());
        reviewed = getContentReader(collectionsPath, get().getReviewedFolderName());
    }

    private ContentReader getContentReader(Path collectionPath, String folderName) throws IOException {

        if (!Files.exists(collectionPath.resolve(folderName))) {
            Files.createDirectory(collectionPath.resolve(folderName));
        }

        return new FileSystemContentReader(collectionPath.resolve(folderName));
    }
}
