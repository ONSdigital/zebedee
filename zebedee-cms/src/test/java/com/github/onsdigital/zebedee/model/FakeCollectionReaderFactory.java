package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReaderFactory;

import java.io.IOException;

public class FakeCollectionReaderFactory implements CollectionReaderFactory {

    private String collectionFolderPath;

    public FakeCollectionReaderFactory(String collectionFolderPath) {
        this.collectionFolderPath = collectionFolderPath;
    }

    @Override
    public CollectionReader createCollectionReader(String collectionId, String sessionId) throws NotFoundException, IOException, BadRequestException, UnauthorizedException {

        // Collection (null check before authorisation check)
        if (collectionId == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (sessionId == null) {
            throw new UnauthorizedException("");
        }

        return new FakeCollectionReader(collectionFolderPath, collectionId);
    }
}
