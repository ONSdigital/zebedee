package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;

import java.io.IOException;

public interface CollectionReaderFactory {

    /**
     * Factory method to create a collection reader instance.
     *
     * @param collectionId - The collection Id to create the reader for.
     * @param sessionId    - The session ID of the user reading the collection.
     * @return
     * @throws NotFoundException
     * @throws IOException
     * @throws BadRequestException
     * @throws UnauthorizedException
     */
    CollectionReader createCollectionReader(String collectionId, String sessionId) throws NotFoundException, IOException, BadRequestException, UnauthorizedException;
}
