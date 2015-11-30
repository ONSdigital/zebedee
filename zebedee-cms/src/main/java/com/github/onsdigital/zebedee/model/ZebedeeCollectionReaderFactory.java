package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Collections;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReaderFactory;

import java.io.IOException;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;

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
        Session session = Root.zebedee.sessions.get(sessionId);
        Collection collection = Collections.getCollection(collectionId);
        return getCollectionReader(collection, session);
    }

    CollectionReader getCollectionReader(Collection collection, Session session) throws BadRequestException, IOException, UnauthorizedException {

        // Collection (null check before authorisation check)
        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null
                || !zebedee.permissions.canView(session.email,
                collection.description)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        return new ZebedeeCollectionReader(zebedee, collection, session);
    }
}
