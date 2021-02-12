package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.cache.KeyringCache;
import com.github.onsdigital.zebedee.user.model.User;

/**
 * CollectionKeyringImpl adds a permissions check wrapper around a {@link KeyringCache} instance to ensure only
 * authorised users can access collection encryption keys
 */
public class CollectionKeyringImpl implements CollectionKeyring {

    static final String USER_NULL_ERR = "user required but was null";
    static final String USER_KEYRING_NULL_ERR = "user keyring required but was null";
    static final String USER_KEYRING_LOCKED_ERR = "error user keyring is locked";
    static final String NOT_INITALISED_ERR = "CollectionKeyring accessed but not yet initalised";
    static final String KEYRING_CACHE_NULL_ERR = "keyringCache required but was null";

    /**
     * Singleton instance.
     */
    private static CollectionKeyring INSTANCE = null;

    private final KeyringCache cache;

    /**
     * CollectionKeyringImpl is a singleton instance. Use {@link CollectionKeyringImpl#init(KeyringCache)} to
     * construct and initialise a new instance. Use {@link CollectionKeyringImpl#getInstance()} to accessed the
     * singleton.
     *
     * @param cache the {@link KeyringCache} instance to use.
     * @throws KeyringException the {@link KeyringCache} was null.
     */
    private CollectionKeyringImpl(KeyringCache cache) throws KeyringException {
        if (cache == null) {
            throw new KeyringException(KEYRING_CACHE_NULL_ERR);
        }

        this.cache = cache;
    }

    @Override
    public void populateFromUser(User user) throws KeyringException {
        if (user == null) {
            throw new KeyringException(USER_NULL_ERR);
        }

        if (user.keyring() == null) {
            throw new KeyringException(USER_KEYRING_NULL_ERR);
        }

        if (!user.keyring().isUnlocked()) {
            throw new KeyringException(USER_KEYRING_LOCKED_ERR);
        }

        if (!user.keyring().list().isEmpty()) {
            for (String collectionID : user.keyring().list()) {
                cache.add(collectionID, user.keyring().get(collectionID));
            }
        }
    }

    /**
     * Initailise the CollectionKeyring.
     *
     * @param keyringCache the {@link KeyringCache} instance to use.
     * @throws KeyringException failed to initialise instance.
     */
    public static void init(KeyringCache keyringCache) throws KeyringException {
        if (INSTANCE == null) {
            synchronized (CollectionKeyringImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CollectionKeyringImpl(keyringCache);
                }
            }
        }
    }

    /**
     * Initalise a NOP impl.
     */
    public static void initNoOp() {
        if (INSTANCE == null) {
            INSTANCE = new NopCollectionKeyring();
        }
    }

    /**
     * @return a singleton instance of the CollectionKeyring
     * @throws KeyringException CollectionKeyring has not been initalised before being accessed.
     */
    public static CollectionKeyring getInstance() throws KeyringException {
        if (INSTANCE == null) {
            throw new KeyringException(NOT_INITALISED_ERR);
        }
        return INSTANCE;
    }
}
