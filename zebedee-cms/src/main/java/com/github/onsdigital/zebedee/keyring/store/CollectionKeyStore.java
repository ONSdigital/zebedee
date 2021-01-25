package com.github.onsdigital.zebedee.keyring.store;

import com.github.onsdigital.zebedee.keyring.KeyringException;

import javax.crypto.SecretKey;

/**
 * Defines the behaviour of a class for storing {@link SecretKey}s.
 */
public interface CollectionKeyStore {

    /**
     * Retrieve a {@link SecretKey} from the store for the specified collection.
     *
     * @param collectionID the ID of the collection to get the key for.
     * @return the {@link SecretKey} used to encrypt the collection content.
     * @throws KeyringException thrown if the requested key does not exist or if there is a problem retrieving the key.
     */
    SecretKey read(String collectionID) throws KeyringException;

    /**
     * Write a collection {@link SecretKey} to the store.
     *
     * @param collectionID  the ID of the collection the key belongs to.
     * @param collectionKey the {@link SecretKey} used to encrypt the collection content.
     * @throws KeyringException problem writing the key to the store, collection key already exists with the
     *                          specified collection ID.
     */
    void write(final String collectionID, final SecretKey collectionKey) throws KeyringException;

    /**
     * Delete a {@link SecretKey} from the store.
     *
     * @param collectionID the collection ID of the key to delete.
     * @throws KeyringException thrown if the requested key does not exist or if there was a problem deleting the key.
     */
    void delete(final String collectionID) throws KeyringException;
}
