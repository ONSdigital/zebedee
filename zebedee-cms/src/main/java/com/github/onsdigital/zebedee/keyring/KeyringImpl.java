package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.store.CollectionKeyStore;
import liquibase.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

/**
 * In memory {@link Keyring} implementation. Keyring uses a {@link CollectionKeyStore} to persist entries to storage
 * whilst maintaining a copy of the data in an in-memory cache for speedy retrieval. If an attempt is made to add a
 * duplicate key the Keyring will compare the new & existing {@link SecretKey} values. If the keys are not equal then a
 * {@link KeyringException} is thrown. This aims to prevent collection key values being overwritten as this will
 * result in a collection that can no longer be decrypted. Otherwise the key values are the same and the entry
 * already exists so no action is taken.
 * <p>
 * This implementation uses a {@link HashMap} as a cache. This approach means all collection keys will be held in
 * memory at once. At the time of writing this Class we don't feel this memory footprint will be problematic:
 * <ul>
 *     <li>There are usually only a small number of collections in existence at any given time.</li>
 *     <li>The size of the data held in the cache is fairly small.</li>
 * </ul>
 * However if this does become an issue consider replacing the Hashmap with some type time based cache object to
 * automatically evicted after a duration of inactivity.
 */
public class KeyringImpl implements Keyring {

    static final String INVALID_COLLECTION_ID_ERR = "expected collection ID but was null or empty";
    static final String INVALID_SECRET_KEY_ERR = "expected secret key but was null";
    static final String KEY_MISMATCH_ERR =
            "add unsuccessful as a different SecretKey already exists for this collection ID";
    static final String KEY_NOT_FOUND_ERR = "SecretKey not found for this collection ID";
    static final String LOAD_KEYS_NULL_ERR = "error loading keystore expected map but was null";

    private CollectionKeyStore keyStore;
    private Map<String, SecretKey> cache;

    /**
     * Create a new instance of the Keyring.
     *
     * @param keyStore {@link CollectionKeyStore} to use to read/write entries to/from persistent storage.
     */
    public KeyringImpl(final CollectionKeyStore keyStore) {
        this(keyStore, new HashMap<>());
    }

    KeyringImpl(final CollectionKeyStore keyStore, final Map<String, SecretKey> cache) {
        this.keyStore = keyStore;
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     * <b>WARNING: This action is destructive</b>. Calling load on a populated keyring will clear all existing values
     * from it before repolating it with the the values returned by {@link CollectionKeyStore#readAll()}. It is
     * strongly advised to only use this method when the keyring is initialised on start up.
     *
     * @throws KeyringException problem loading the keyring.
     */
    @Override
    public synchronized void load() throws KeyringException {
        Map<String, SecretKey> keyMapping = keyStore.readAll();
        if (keyMapping == null) {
            throw new KeyringException(LOAD_KEYS_NULL_ERR);
        }

        if (!keyMapping.isEmpty()) {
            cache.clear();
            cache.putAll(keyMapping);
        }
    }

    @Override
    public synchronized void add(final String collectionID, final SecretKey secretKey) throws KeyringException {
        validateAddKeyParams(collectionID, secretKey);

        if (keyExistsInCache(collectionID, secretKey)) {
            return;
        }

        if (keyExistsInStore(collectionID, secretKey)) {
            cache.put(collectionID, secretKey);
            return;
        }

        keyStore.write(collectionID, secretKey);
        cache.put(collectionID, secretKey);
    }

    private void validateAddKeyParams(String collectionID, SecretKey secretKey) throws KeyringException {
        if (StringUtils.isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        if (secretKey == null) {
            throw new KeyringException(INVALID_SECRET_KEY_ERR, collectionID);
        }
    }

    /**
     * Check if an entry for this collection ID already exists in the cache. If so retieve the entry from the cache
     * and check the existing key matches the key being added.
     *
     * @param collectionID the collection ID the entry is being added against.
     * @param keyToAdd     the {@link SecretKey} used to encrypt the collection content.
     * @return false if there is no existing entry in the cache for this collection ID. Return true if an entry for
     * this collection ID already exists and the new & existing keys are equal.
     * @throws KeyringException thrown if an entry already exists for this collection ID but the new key does not
     *                          equal the existing key.
     */
    private boolean keyExistsInCache(String collectionID, SecretKey keyToAdd) throws KeyringException {
        if (!cache.containsKey(collectionID)) {
            return false;
        }

        SecretKey cachedKey = cache.get(collectionID);
        if (keyToAdd.equals(cachedKey)) {
            return true;
        }

        throw new KeyringException(KEY_MISMATCH_ERR, collectionID);
    }

    /**
     * Check if an entry for this collection ID already exists in the {@link CollectionKeyStore}. If so retieve the entry from the
     * store and check the existing key matches the key being added.
     *
     * @param collectionID the collection ID the entry is being added against.
     * @param secretKey    the {@link SecretKey} used to encrypt the collection content.
     * @return false if there is no existing entry in the store for this collection ID. Return true if an entry for
     * this collection ID already exists and the new & existing keys are equal.
     * @throws KeyringException thrown if an entry already exists for this collection ID but the new key does not
     *                          equal the existing key.
     */
    private boolean keyExistsInStore(String collectionID, SecretKey secretKey) throws KeyringException {
        if (!keyStore.exists(collectionID)) {
            return false;
        }

        SecretKey existingKey = keyStore.read(collectionID);
        if (secretKey.equals(existingKey)) {
            return true;
        }

        throw new KeyringException(KEY_MISMATCH_ERR, collectionID);
    }

    @Override
    public synchronized SecretKey get(String collectionID) throws KeyringException {
        if (StringUtils.isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        if (cache.containsKey(collectionID)) {
            return cache.get(collectionID);
        }

        if (!keyStore.exists(collectionID)) {
            throw new KeyringException(KEY_NOT_FOUND_ERR, collectionID);
        }

        SecretKey key = keyStore.read(collectionID);
        cache.put(collectionID, key);

        return key;
    }

    @Override
    public synchronized void remove(String collectionID) throws KeyringException {
        if (StringUtils.isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        if (!keyStore.exists(collectionID)) {
            throw new KeyringException(KEY_NOT_FOUND_ERR);
        }

        keyStore.delete(collectionID);
        cache.remove(collectionID);
    }
}
