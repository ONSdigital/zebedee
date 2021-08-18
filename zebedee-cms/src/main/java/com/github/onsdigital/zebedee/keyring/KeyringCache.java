package com.github.onsdigital.zebedee.keyring;

import javax.crypto.SecretKey;
import java.util.Set;

/**
 * A Keyring is an object repsonsible for holding {@link SecretKey}s used to encrypt/decrypt the content of
 * {@link com.github.onsdigital.zebedee.model.Collection}s. Provides methods to add, retrieve and remove keys to/from
 * the keyring.
 */
public interface KeyringCache extends SchedulerKeyCache {

    /**
     * Populate the keyring.
     *
     * @throws KeyringException problem populating the keyring.
     */
    void load() throws KeyringException;

    /**
     * Remove an entry from the Keyring.
     *
     * @param collectionID the collection ID of the entry to be removed.
     * @throws KeyringException problem removing the entry
     */
    void remove(final String collectionID) throws KeyringException;

    /**
     * Returns a list of collection IDs from the Keyring
     *
     * @return a set of collection IDs
     * @throws KeyringException error listing keys
     */
    Set<String> list() throws KeyringException;
}
