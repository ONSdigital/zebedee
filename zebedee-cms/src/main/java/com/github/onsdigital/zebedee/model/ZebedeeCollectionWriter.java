package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.io.IOException;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.get;

/**
 * Zebedee specific CollectionWriter implementation.
 */
public class ZebedeeCollectionWriter extends CollectionWriter {

    static final String ZEBEDEE_NULL_ERR =
            "error constructing ZebedeeCollectionWriter zebedee instance required but was null";
    static final String PERMISSIONS_SERVICE_NULL_ERR =
            "error constructing ZebedeeCollectionWriter perissions service required but was null";
    static final String KEYRING_NULL_ERR =
            "error constructing ZebedeeCollectionWriter keyring requred but was null";
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

    /**
     * Deprecated do not use this constructor.
     */
    @Deprecated
    public ZebedeeCollectionWriter(Zebedee zebedee, Collection collection, Session session)
            throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        if (collection == null) {
            throw new NotFoundException(COLLECTION_NULL_ERR);
        }

        // Authorisation
        if (session == null || !zebedee.getPermissionsService().canEdit(session.getEmail(), collection.getDescription())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        Keyring keyring = zebedee.getLegacyKeyringCache().get(session);
        if (keyring == null) throw new UnauthorizedException("No keyring is available for " + session.getEmail());

        SecretKey key = keyring.get(collection.getDescription().getId());
        init(collection, key);
    }

    /**
     * Create a new instance of CollectionWriter for the given Zebedee instance, collection, and session.
     */
    public ZebedeeCollectionWriter(Zebedee zebedee, Collection collection, User user)
            throws BadRequestException, IOException, UnauthorizedException, NotFoundException {
        validateParams(zebedee, collection, user);
        checkUserAuthorisedToAccessCollection(zebedee, collection, user);
        SecretKey key = getCollectionKey(zebedee, collection, user);
        init(collection, key);
    }

    private void validateParams(Zebedee zebedee, Collection collection, User user)
            throws IOException, UnauthorizedException, NotFoundException {
        if (zebedee == null) {
            throw new IOException(ZEBEDEE_NULL_ERR);
        }

        if (zebedee.getPermissionsService() == null) {
            throw new IOException(PERMISSIONS_SERVICE_NULL_ERR);
        }

        if (zebedee.getCollectionKeyring() == null) {
            throw new IOException(KEYRING_NULL_ERR);
        }

        if (collection == null) {
            throw new NotFoundException(COLLECTION_NULL_ERR);
        }

        if (user == null) {
            throw new UnauthorizedException(USER_NULL_ERR);
        }
    }

    private void checkUserAuthorisedToAccessCollection(Zebedee zebedee, Collection collection, User user)
            throws IOException, UnauthorizedException {
        boolean isAuthorised = false;
        try {
            isAuthorised = zebedee.getPermissionsService().canEdit(user, collection.getDescription());
        } catch (Exception ex) {
            throw new IOException(PERMISSIONS_CHECK_ERR, ex);
        }

        if (!isAuthorised) {
            throw new UnauthorizedException(PERMISSION_DENIED_ERR);
        }
    }

    private SecretKey getCollectionKey(Zebedee zebedee, Collection collection, User user)
            throws KeyringException, UnauthorizedException {
        SecretKey key = zebedee.getCollectionKeyring().get(user, collection);

        if (key == null) {
            throw new UnauthorizedException(COLLECTION_KEY_NULL_ERR);
        }

        return key;
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
        init(collection, key);
    }

    private void init(Collection collection, SecretKey key)
            throws NotFoundException, UnauthorizedException, IOException {

        if (collection == null) {
            throw new NotFoundException("Please specify a collection");
        }

        ReaderConfiguration cfg = get();

        this.inProgress = new CollectionContentWriter(
                collection, key, collection.getPath().resolve(cfg.getInProgressFolderName()));

        this.complete = new CollectionContentWriter(collection, key,
                collection.getPath().resolve(cfg.getCompleteFolderName()));

        this.reviewed = new CollectionContentWriter(collection, key,
                collection.getPath().resolve(cfg.getReviewedFolderName()));

        this.root = new CollectionContentWriter(collection, key, collection.getPath());
    }
}