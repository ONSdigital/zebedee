package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.io.IOException;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.get;

/**
 * Zebedee specific CollectionWriter implementation.
 */
public class ZebedeeCollectionWriter extends CollectionWriter {

    static final String ZEBEDEE_NULL_ERR =
            "error constructing ZebedeeCollectionWriter zebedee instance required but was null";
    static final String PERMISSIONS_SERVICE_NULL_ERR =
            "error constructing ZebedeeCollectionWriter perissions service required but was null";
    static final String USERS_SERVICE_NULL_ERR =
            "error constructing ZebedeeCollectionWriter users service required but was null";
    static final String KEYRING_NULL_ERR =
            "error constructing ZebedeeCollectionWriter keyring requred but was null";
    static final String SESSION_NULL_ERR =
            "error constructing ZebedeeCollectionWriter session required but was null";
    static final String GET_USER_ERR =
            "error constructing ZebedeeCollectionWriter error getting user ";
    static final String USER_NULL_ERR =
            "error constructing ZebedeeCollectionWriter user required but was null";
    static final String COLLECTION_NULL_ERR =
            "error constructing ZebedeeCollectionWriter collection required but was null";
    static final String PERMISSION_DENIED_ERR =
            "error constructing ZebedeeCollectionWriter user does not have edit permission for collection";
    static final String PERMISSIONS_CHECK_ERR =
            "error constructing ZebedeeCollectionWriter error checking user collection permissions";
    static final String COLLECTION_KEY_NULL_ERR =
            "error constructing ZebedeeCollectionWriter key required but keyring returned null";

    private Zebedee zebedee;

    /**
     * Create a new instance of CollectionWriter for the given Zebedee instance, collection, and session.
     */
    public ZebedeeCollectionWriter(Zebedee zebedee, Collection collection, Session session)
            throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        validateParams(zebedee, collection, session);
        this.zebedee = zebedee;

        checkUserAuthorisedToAccessCollection(zebedee, session);

        SecretKey key = getCollectionKey(zebedee, collection, session);

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
    public ZebedeeCollectionWriter(Collection collection, SecretKey key)
            throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        this.zebedee = Root.zebedee;
        init(collection, key);
    }

    private void validateParams(Zebedee zebedee, Collection collection, Session session)
            throws IOException, UnauthorizedException, NotFoundException {
        if (zebedee == null) {
            throw new IOException(ZEBEDEE_NULL_ERR);
        }

        if (zebedee.getPermissionsService() == null) {
            throw new IOException(PERMISSIONS_SERVICE_NULL_ERR);
        }

        if (zebedee.getUsersService() == null) {
            throw new IOException(USERS_SERVICE_NULL_ERR);
        }

        if (zebedee.getCollectionKeyring() == null) {
            throw new IOException(KEYRING_NULL_ERR);
        }

        if (collection == null) {
            throw new NotFoundException(COLLECTION_NULL_ERR);
        }

        if (session == null) {
            throw new UnauthorizedException(SESSION_NULL_ERR);
        }
    }

    private void checkUserAuthorisedToAccessCollection(Zebedee zebedee, Session session)
            throws IOException, UnauthorizedException {
        boolean isAuthorised = false;
        try {
            isAuthorised = zebedee.getPermissionsService().canEdit(session);
        } catch (Exception ex) {
            throw new IOException(PERMISSIONS_CHECK_ERR, ex);
        }

        if (!isAuthorised) {
            throw new UnauthorizedException(PERMISSION_DENIED_ERR);
        }
    }

    private SecretKey getCollectionKey(Zebedee zebedee, Collection collection, Session session)
            throws KeyringException, UnauthorizedException {
        SecretKey key = zebedee.getCollectionKeyring().get(session, collection);

        if (key == null) {
            throw new UnauthorizedException(COLLECTION_KEY_NULL_ERR);
        }

        return key;
    }

    private void init(Collection collection, SecretKey key)
            throws NotFoundException, UnauthorizedException, IOException {

        if (collection == null) {
            throw new NotFoundException("Please specify a collection");
        }

        ReaderConfiguration cfg = get();

        this.inProgress = new CollectionContentWriter(
                collection, key, collection.getPath().resolve(cfg.getInProgressFolderName()), zebedee.getSlackNotifier());

        this.complete = new CollectionContentWriter(collection, key,
                collection.getPath().resolve(cfg.getCompleteFolderName()), zebedee.getSlackNotifier());

        this.reviewed = new CollectionContentWriter(collection, key,
                collection.getPath().resolve(cfg.getReviewedFolderName()), zebedee.getSlackNotifier());

        root = new CollectionContentWriter(collection, key, collection.getPath(), zebedee.getSlackNotifier());
    }
}
