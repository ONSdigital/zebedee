package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import java.io.IOException;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

/**
 * Zebedee specific CollectionWriter implementation.
 */
public class ZebedeeCollectionWriter extends CollectionWriter {

    /**
     * Create a new instance of CollectionWriter for the given Zebedee instance, collection, and session.
     *
     * @param zebedee
     * @param collection
     * @param session
     * @throws BadRequestException
     * @throws IOException
     * @throws UnauthorizedException
     */
    public ZebedeeCollectionWriter(Zebedee zebedee, Collection collection, Session session) throws BadRequestException, IOException, UnauthorizedException {

        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }
        // Authorisation
        if (session == null
                || !zebedee.permissions.canEdit(session.email,
                collection.description)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        ReaderConfiguration config = getConfiguration();

        inProgress = new CollectionContentWriter(zebedee, collection, session, collection.path.resolve(config.getInProgressFolderName()));
        complete = new CollectionContentWriter(zebedee, collection, session, collection.path.resolve(config.getCompleteFolderName()));
        reviewed = new CollectionContentWriter(zebedee, collection, session, collection.path.resolve(config.getReviewedFolderName()));
    }
}
