package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.user.model.User;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Factory encapsulating the creation of {@link CollectionReader}.
 */
public class CollectionReaderWriterFactory {

    /**
     * Get a new instance of {@link ZebedeeCollectionWriter}.
     *
     * @param zebedee    the {@link Zebedee} instance to use.
     * @param collection the {@link Collection} to write {@link Content} to.
     * @param session    the {@link Session} of the {@link com.github.onsdigital.zebedee.json.User} making the change.
     * @return a new instance of {@link ZebedeeCollectionWriter}.
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws IOException
     */
    public CollectionWriter getWriter(Zebedee zebedee, Collection collection, Session session) throws NotFoundException,
            BadRequestException, UnauthorizedException, IOException {
        requireNonNull(zebedee, "Zebedee is a required parameter for CollectionReader.");
        requireNonNull(collection, "Collections is a required parameter for CollectionReader.");
        requireNonNull(session, "Session is a required parameter for CollectionReader.");
        return new ZebedeeCollectionWriter(zebedee, collection, session);
    }

    public CollectionReader getReader(Zebedee zebedee, Collection collection, Session session) throws NotFoundException,
            BadRequestException, UnauthorizedException, IOException {
        requireNonNull(zebedee, "Zebedee is a required parameter for CollectionReader.");
        requireNonNull(collection, "Collections is a required parameter for CollectionReader.");
        requireNonNull(session, "Session is a required parameter for CollectionReader.");
        return new ZebedeeCollectionReader(zebedee, collection, session);
    }
}
