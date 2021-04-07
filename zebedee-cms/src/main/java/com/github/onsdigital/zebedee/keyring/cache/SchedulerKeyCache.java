package com.github.onsdigital.zebedee.keyring.cache;

import com.github.onsdigital.zebedee.keyring.KeyringException;

import javax.crypto.SecretKey;

/**
 * Defines a {@link com.github.onsdigital.zebedee.keyring.Keyring} cache used for scheduled publishing tasks (i.e.
 * non user driven actions).
 */
public interface SchedulerKeyCache {

    /**
     * Get a {@link SecretKey} from the cache.
     *
     * @param collectionID the collection ID the key belongs to.
     * @return the key if it exists.
     * @throws KeyringException problem getting the key.
     */
    SecretKey get(String collectionID) throws KeyringException;

    /**
     * Add a {@link SecretKey} to the scheduler cache.
     *
     * @param collectionID the ID of the collection the key belongs to.
     * @param key          the key to add.
     * @throws KeyringException problem adding the key.
     */
    void add(String collectionID, SecretKey key) throws KeyringException;
}
