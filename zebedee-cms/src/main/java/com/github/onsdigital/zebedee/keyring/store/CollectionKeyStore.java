package com.github.onsdigital.zebedee.keyring.store;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.github.onsdigital.zebedee.keyring.KeyringException;

/**
 * Defines the behaviour of a Collection encryption key store.
 */
public interface CollectionKeyStore {

    /**
     * Read a {@link CollectionKey} from the store.
     *
     * @param collectionID the {@link com.github.onsdigital.zebedee.model.Collection} ID of the encryption key to
     *                     retrieve.
     * @return the {@link CollectionKey} if it exists.
     * @throws KeyringException thrown if the requested key does not exist or if there is a problem retrieving the key.
     */
    CollectionKey read(String collectionID) throws KeyringException;

    void write(CollectionKey key) throws KeyringException;
}
