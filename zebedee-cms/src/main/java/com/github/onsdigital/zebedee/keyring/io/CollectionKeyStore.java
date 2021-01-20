package com.github.onsdigital.zebedee.keyring.io;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.github.onsdigital.zebedee.keyring.KeyringException;

/**
 * Defines the behaviour of a {@link CollectionKey} storage mechanism.
 */
public interface CollectionKeyStore {

    /**
     * Read a {@link CollectionKey} with the specified collection ID from the store if it exists.
     *
     * @param collectionID the {@link com.github.onsdigital.zebedee.model.Collection} ID of the collection key to read.
     * @return the {@link CollectionKey} if it exists.
     * @throws KeyringException thrown if the requested key does not exist or if there is a problem retrieving the key.
     */
    CollectionKey read(String collectionID) throws KeyringException;

    /**
     * Write a {@link CollectionKey} to the store
     *
     * @param key the {@link CollectionKey} to write.
     * @throws KeyringException problem writing the key to the store, collection key already exists with the
     *                          specified collection ID.
     */
    void write(final CollectionKey key) throws KeyringException;

    /**
     * Delete a {@link CollectionKey} from the store.
     *
     * @param collectionID the collection ID of the key to delete.
     * @throws KeyringException thrown if the requested key does not exist or if there was a problem deleting the key.
     */
    void delete(final String collectionID) throws KeyringException;
}
