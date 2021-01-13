package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;

public interface KeyringService {

    /**
     * Added a new {@link Collection} encryption key to the keyring.
     *
     * @param collection the {@link Collection} the encryption key belongs to (required, not null).
     * @param collectionKey  the {@link SecretKey} to decrypt the collection content.
     * @throws KeyringException problem adding secret key to keyring.
     */
    void add(final Collection collection, final SecretKey collectionKey) throws KeyringException;

    /**
     * Get the encrption key for the specified collection if it exists in the keyring. Otherwise return null.
     *
     * @param collection the {@link Collection} get the encyption key for (required, not null).
     * @return
     * @throws KeyringException problem getting the {@link SecretKey} for the collection.
     */
    SecretKey get(final Collection collection) throws KeyringException;

    /**
     * @param collection
     * @return
     * @throws KeyringException
     */
    boolean remove(final Collection collection) throws KeyringException;
}
