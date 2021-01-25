package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.store.CollectionKeyStore;
import liquibase.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

public class KeyringImpl implements Keyring {

    static final String INVALID_COLLECTION_ID_ERR_MSG = "expected collection ID but was null or empty";
    static final String INVALID_SECRET_KEY_ERR_MSG = "expected secret key but was null";
    static final String DUPLICATE_KEY_ERR_MSG = "collection key already exists for this collection";
    static final String KEY_MISMATCH_ERR_MSG =
            "add unsuccessful a different SecretKey already exists for a collection with ID";
    static final String KEY_NOT_FOUND_ERR_MSG = "collectionKey not found for this collection ID";

    private CollectionKeyStore keyStore;
    private Map<String, SecretKey> cache;

    public KeyringImpl(final CollectionKeyStore keyStore) {
        this(keyStore, new HashMap<>());
    }

    KeyringImpl(final CollectionKeyStore keyStore, final Map<String, SecretKey> cache) {
        this.keyStore = keyStore;
        this.cache = cache;
    }

    @Override
    public synchronized void add(final String collectionID, final SecretKey secretKey) throws KeyringException {
        validateAddKeyParams(collectionID, secretKey);

        if (!isCached(collectionID, secretKey)) {
            keyStore.write(collectionID, secretKey);
            cache.put(collectionID, secretKey);
        }
    }

    private void validateAddKeyParams(String collectionID, SecretKey secretKey) throws KeyringException {
        if (StringUtils.isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR_MSG);
        }

        if (secretKey == null) {
            throw new KeyringException(INVALID_SECRET_KEY_ERR_MSG, collectionID);
        }
    }

    private boolean isCached(String collectionID, SecretKey keyToAdd) throws KeyringException {
        if (!cache.containsKey(collectionID)) {
            return false;
        }

        SecretKey cachedKey = cache.get(collectionID);
        if (keyToAdd.equals(cachedKey)) {
            return true;
        }

        throw new KeyringException(KEY_MISMATCH_ERR_MSG, collectionID);
    }

    @Override
    public synchronized SecretKey get(String collectionID) throws KeyringException {
        return null;
    }

    @Override
    public synchronized void remove(String collectionID) throws KeyringException {

    }
}
