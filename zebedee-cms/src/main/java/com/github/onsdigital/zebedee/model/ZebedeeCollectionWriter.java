package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import javax.crypto.SecretKey;
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
    public ZebedeeCollectionWriter(Zebedee zebedee, Collection collection, Session session) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {

        if (collection == null) {
            throw new NotFoundException("Please specify a collection");
        }

        // Authorisation
        if (session == null || !zebedee.getPermissionsService().canEdit(session.getEmail(), collection.getDescription())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        Keyring keyring = zebedee.getKeyringCache().get(session);
        if (keyring == null) throw new UnauthorizedException("No keyring is available for " + session.getEmail());

        SecretKey key = keyring.get(collection.getDescription().getId());
        init(collection, key);
    }

    /**
     * Create a new instance of CollectionWriter for the given SecretKey if you already have it.
     *
     * @param collection
     * @param key
     * @throws BadRequestException
     * @throws IOException
     * @throws UnauthorizedException
     * @throws NotFoundException
     */
    public ZebedeeCollectionWriter(Collection collection, SecretKey key) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        init(collection, key);
    }

    private void init(Collection collection, SecretKey key) throws NotFoundException, UnauthorizedException, IOException {

        if (collection == null) {
            throw new NotFoundException("Please specify a collection");
        }

        ReaderConfiguration config = getConfiguration();

        inProgress = new CollectionContentWriter(collection, key, collection.path.resolve(config.getInProgressFolderName()));
        complete = new CollectionContentWriter(collection, key, collection.path.resolve(config.getCompleteFolderName()));
        reviewed = new CollectionContentWriter(collection, key, collection.path.resolve(config.getReviewedFolderName()));
        root = new CollectionContentWriter(collection, key, collection.path);
    }
}
