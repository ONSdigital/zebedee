package com.github.onsdigital.zebedee.keyring.io;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.github.onsdigital.zebedee.keyring.KeyringException;

/**
 * A {@link CollectionKey} read writer is used to read and write Collection keys to/from encrypted files on disk.
 */
public interface CollectionKeyReadWriter {

    /**
     * Read a {@link CollectionKey} with the specified collection ID from the file system if it exists.
     *
     * @param collectionID the {@link com.github.onsdigital.zebedee.model.Collection} ID of the collection key to read.
     * @return the {@link CollectionKey} if it exists.
     * @throws KeyringException thrown if the requested key does not exist or if there is a problem retrieving the key.
     */
    CollectionKey read(String collectionID) throws KeyringException;

    /**
     * Write a {@link CollectionKey} to an encrypted file.
     *
     * @param key the {@link CollectionKey} to write.
     * @throws KeyringException problem writing the key to the store.
     */
    void write(final CollectionKey key) throws KeyringException;
}
