package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;

/**
 * Defines the behaviour of a keyring for holding {@link Collection} encryption keys.
 */
public interface Keyring {

    /**
     * Add a new collection encryption key to the keyring.
     *
     * @param collection the {@link Collection} the key belongs too. Required/not null.
     * @param secretKey  the {@link SecretKey} to encrypt/decrypt the collection content.
     * @throws KeyringException throw if there is any issue adding the key to the keyring.
     */
    void add(final Collection collection, final SecretKey secretKey) throws KeyringException;

    /**
     * Get the {@link SecretKey} used to encyrpt/decrypt the specific collection from the keyring if it exists.
     *
     * @param collection the {@link Collection} to get the key for.:th
     * @return the {@link SecretKey} for the specified collection if it exists.
     * @throws KeyringException problem getting the secret key.
     */
    SecretKey get(final Collection collection) throws KeyringException;

    /**
     * Remove a {@link SecretKey} from the keyring.
     *
     * @param collection the {@link Collection} the {@link SecretKey} belongs to.
     * @throws KeyringException problem removing the key
     */
    void remove(final Collection collection) throws KeyringException;
}
