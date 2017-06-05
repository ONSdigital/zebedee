package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReaderFactory;

import java.io.IOException;

/**
 * Creates instances of CollectionReader.
 */
public class ZebedeeCollectionReaderFactory implements CollectionReaderFactory {

    private Zebedee zebedee;

    public ZebedeeCollectionReaderFactory(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

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
    @Override
    public CollectionReader createCollectionReader(String collectionId, String sessionId) throws NotFoundException, IOException, BadRequestException, UnauthorizedException {
        Session session = zebedee.getSessionsService().get(sessionId);
        Collection collection = zebedee.getCollections().getCollection(collectionId);
        return getCollectionReader(collection, session);
    }

    CollectionReader getCollectionReader(Collection collection, Session session) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        return new ZebedeeCollectionReader(zebedee, collection, session);
    }
}
