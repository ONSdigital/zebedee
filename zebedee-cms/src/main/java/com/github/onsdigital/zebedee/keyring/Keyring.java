package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;

public interface Keyring {

    /**
     * Populate the Keyring from an unlocked {@link User#keyring()}
     *
     * @param user the user that populates the keyring.
     * @throws KeyringException problem populating the keyring.
     */
    void populateFromUser(User user) throws KeyringException;

    /**
     * Get a key from the keyring for the specified collection.
     *
     * @param user       the user requesting the key.
     * @param collection the collection to get the key for.
     * @return the collection key if it exists null if not.
     * @throws KeyringException problem getting the key.
     */
    SecretKey get(User user, Collection collection) throws KeyringException;

    /**
     * Remove a key from the keyring.
     *
     * @param user       the user performing the action.
     * @param collection the collection the the key belongs to.
     * @throws KeyringException problem removing the key.
     */
    void remove(User user, Collection collection) throws KeyringException;

    /**
     * Add a key to the keyring.
     *
     * @param user       the user adding the key.
     * @param collection the {@link Collection} the key belongs to.
     * @param key        the {@link SecretKey} to add.
     * @throws KeyringException problem adding the key to the keyring.
     */
    void add(User user, Collection collection, SecretKey key) throws KeyringException;
}
