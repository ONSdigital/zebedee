package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

/**
 * I dont care for all this collection stuff, I just want a CollectionWriter instance.
 */
public class DummyCollectionReader extends CollectionReader {

    private Path collections;

    public DummyCollectionReader(Path collectionsPath) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        inProgress = getContentReader(collectionsPath, getConfiguration().getInProgressFolderName());
        complete = getContentReader(collectionsPath, getConfiguration().getCompleteFolderName());
        reviewed = getContentReader(collectionsPath, getConfiguration().getReviewedFolderName());
    }

    private ContentReader getContentReader(Path collectionPath, String folderName) throws IOException {

        if (!Files.exists(collectionPath.resolve(folderName))) {
            Files.createDirectory(collectionPath.resolve(folderName));
        }

        return new FileSystemContentReader(collectionPath.resolve(folderName));
    }
}
