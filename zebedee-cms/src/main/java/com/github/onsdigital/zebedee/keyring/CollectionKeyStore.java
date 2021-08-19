package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.KeyringException;

import javax.crypto.SecretKey;
import java.util.Map;

/**
 * Defines the behaviour of a class for storing {@link SecretKey}s.
 */
public interface CollectionKeyStore {

    /**
     * Read and decrypt all collection key files into a {@link java.util.HashMap}.
     *
     * @return {@link java.util.HashMap} mapping collectionID -> SecretKey.
     * @throws KeyringException problem reading the collection key files.
     */
    Map<String, SecretKey> readAll() throws KeyringException;

    /**
     * Check if a collection key file exists for this collection ID.
     *
     * @param collectionID the collection ID to look for.
     * @return true of a key file exists with this collection ID, false otherwise.
     * @throws KeyringException problem checking the key file.
     */
    boolean exists(String collectionID) throws KeyringException;

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
