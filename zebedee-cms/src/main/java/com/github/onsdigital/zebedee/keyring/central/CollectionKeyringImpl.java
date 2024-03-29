package com.github.onsdigital.zebedee.keyring.central;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * CollectionKeyringImpl adds a permissions check wrapper around a {@link CollectionKeyCache} instance to ensure only
 * authorised users can access collection encryption keys
 */
public class CollectionKeyringImpl implements CollectionKeyring {

    static final String SESSION_NULL_ERR = "user session required but was null";
    static final String SESSION_EMAIL_ERR = "user email required but was null or empty";
    static final String NOT_INITIALISED_ERR = "CollectionKeyring accessed but not yet initialised";
    static final String KEYRING_CACHE_NULL_ERR = "keyringCache required but was null";
    static final String COLLECTION_NULL_ERR = "collection required but was null";
    static final String COLLECTION_DESCRIPTION_NULL_ERR = "collection description required but is null";
    static final String COLLECTION_ID_NULL_OR_EMPTY_ERR = "collection ID required but was null or empty";
    static final String PERMISSION_SERVICE_NULL_ERR = "permissionsService required but was null";
    static final String COLLECTIONS_NULL_ERR = "collections service required but was null";
    static final String SECRET_KEY_NULL_ERR = "secret key required but was null";
    static final String FILTER_COLLECTIONS_ERR = "error filtering collections";

    /**
     * Singleton instance.
     */
    private static CollectionKeyring INSTANCE = null;

    private final CollectionKeyCache keyCache;
    private final PermissionsService permissionsService;
    private final Collections collections;

    /**
     * CollectionKeyringImpl is a singleton instance. Use
     * {@link CollectionKeyringImpl#init(CollectionKeyCache, PermissionsService)} to construct and initialise a new
     * instance. Use {@link CollectionKeyringImpl#getInstance()} to accessed the singleton.
     *
     * @param keyCache the {@link CollectionKeyCache} instance to use.
     * @throws KeyringException the {@link CollectionKeyCache} was null.
     */
    private CollectionKeyringImpl(CollectionKeyCache keyCache, PermissionsService permissionsService,
                                  Collections collections) throws KeyringException {
        if (keyCache == null) {
            throw new KeyringException(KEYRING_CACHE_NULL_ERR);
        }

        if (permissionsService == null) {
            throw new KeyringException(PERMISSION_SERVICE_NULL_ERR);
        }

        if (collections == null) {
            throw new KeyringException(COLLECTIONS_NULL_ERR);
        }

        this.permissionsService = permissionsService;
        this.keyCache = keyCache;
        this.collections = collections;
    }

    @Override
    public SecretKey get(Session session, Collection collection) throws KeyringException {
        validateSession(session);
        validateCollection(collection);

        boolean hasPermission = hasViewPermissions(session, collection.getDescription().getId());

        if (!hasPermission) {
            return null;
        }

        return keyCache.get(collection.getDescription().getId());
    }

    @Override
    public void remove(Session session, Collection collection) throws KeyringException {
        validateSession(session);
        validateCollection(collection);

        boolean hasPermission = hasEditPermissions(session);

        if (!hasPermission) {
            return;
        }

        keyCache.remove(collection.getDescription().getId());
    }

    @Override
    public void add(Session session, Collection collection, SecretKey key) throws KeyringException {
        validateSession(session);
        validateCollection(collection);
        validateKey(key);

        boolean hasPermission = hasEditPermissions(session);

        if (!hasPermission) {
            return;
        }

        keyCache.add(collection.getDescription().getId(), key);
    }


    @Override
    public Set<String> list(Session session) throws KeyringException {
        validateSession(session);
        // if admin or editor return all.
        if (hasEditPermissions(session)) {
            return keyCache.list();
        }

        List<String> accessibleCollectionIDs = getCollectionIDsAccessibleByUser(session);

        // Return only the entries the user has access to.
        return keyCache.list()
                .stream()
                .filter(c -> accessibleCollectionIDs.contains(c))
                .collect(Collectors.toSet());
    }

    private void validateSession(Session session) throws KeyringException {
        if (session == null) {
            throw new KeyringException(SESSION_NULL_ERR);
        }

        if (StringUtils.isEmpty(session.getEmail())) {
            throw new KeyringException(SESSION_EMAIL_ERR);
        }
    }

    private void validateKey(SecretKey key) throws KeyringException {
        if (key == null) {
            throw new KeyringException(SECRET_KEY_NULL_ERR);
        }
    }


    private void validateCollection(Collection collection) throws KeyringException {
        if (collection == null) {
            throw new KeyringException(COLLECTION_NULL_ERR);
        }

        if (collection.getDescription() == null) {
            throw new KeyringException(COLLECTION_DESCRIPTION_NULL_ERR);
        }

        if (StringUtils.isEmpty(collection.getDescription().getId())) {
            throw new KeyringException(COLLECTION_ID_NULL_OR_EMPTY_ERR);
        }
    }

    private boolean hasViewPermissions(Session session, String collectionId) throws KeyringException {
        try {
            return permissionsService.canView(session, collectionId);
        } catch (IOException ex) {
            throw new KeyringException(ex);
        }
    }

    private boolean hasEditPermissions(Session session) throws KeyringException {
        try {
            return permissionsService.canEdit(session);
        } catch (IOException ex) {
            throw new KeyringException(ex);
        }
    }

    /**
     * Returns a list of collection IDs that the user has view permission for.
     */
    private List<String> getCollectionIDsAccessibleByUser(Session s) throws KeyringException {
        try {
            // Filter the full list of collections to only those the user has view permisison for. Then Create a list of
            // collection ID strings from this output using the stream.map function.
            return collections.filterBy(userHasViewPermission(s))
                    .stream()
                    .map(c -> c.getId())
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            throw new KeyringException(FILTER_COLLECTIONS_ERR, ex);
        }
    }

    private Predicate<Collection> userHasViewPermission(Session session) {
        return (c) -> {
            try {
                return permissionsService.canView(session, c.getDescription().getId());
            } catch (Exception ex) {
                throw new RuntimeException("error checking user view permission for collection " + c.getId());
            }
        };
    }

    /**
     * Initailise the CollectionKeyring.
     *
     * @param collectionKeyCache the {@link CollectionKeyCache} instance to use.
     * @throws KeyringException failed to initialise instance.
     */
    public static void init(CollectionKeyCache collectionKeyCache, PermissionsService permissionsService,
                            Collections collections) throws KeyringException {
        if (INSTANCE == null) {
            synchronized (CollectionKeyringImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CollectionKeyringImpl(collectionKeyCache, permissionsService, collections);
                }
            }
        }
    }

    /**
     * @return a singleton instance of the CollectionKeyring
     * @throws KeyringException CollectionKeyring has not been initalised before being accessed.
     */
    public static CollectionKeyring getInstance() throws KeyringException {
        if (INSTANCE == null) {
            throw new KeyringException(NOT_INITIALISED_ERR);
        }
        return INSTANCE;
    }
}
