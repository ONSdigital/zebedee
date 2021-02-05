package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;

/**
 * A Keyring is an object repsonsible for holding {@link SecretKey}s used to encrypt/decrypt the content of
 * {@link com.github.onsdigital.zebedee.model.Collection}s. Provides methods to add, retrieve and remove keys to/from
 * the keyring.
 */
public interface Keyring {

    /**
     * Populate the keyring.
     *
     * @throws KeyringException problem populating the keyring.
     */
    void load() throws KeyringException;

    /**
     * Add a entry to the Keyring.
     *
     * @param collectionID  the collection ID the {@link SecretKey} belongs to.
     * @param collectionKey the {@link SecretKey} used to encrypt the collection with this ID.
     * @throws KeyringException problem adding the entry to the keyring.
     */
    void add(final String collectionID, final SecretKey collectionKey) throws KeyringException;

    /**
     * Get the {@link SecretKey} for the provided collection ID.
     *
     * @param collectionID the ID of the {@link Collection} to get the key for.
     * @return the {@link SecretKey} used to encrypt/decrypt the content of this collection.
     * @throws KeyringException problem getting the key from the keyring.
     */
    SecretKey get(final String collectionID) throws KeyringException;

    /**
     * Remove an entry from the Keyring.
     *
     * @param collectionID the collection ID of the entry to be removed.
     * @throws KeyringException problem removing the entry
     */
    void remove(final String collectionID) throws KeyringException;
}
