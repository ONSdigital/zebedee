package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Set;

public interface Keyring {

    /**
     * Populate the Keyring from an unlocked {@link User#keyring()}
     *
     * @param user the user that populates the keyring.
     * @throws KeyringException problem populating the keyring.
     */
    void cacheKeyring(User user) throws KeyringException;

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

    /**
     * Lists the collection IDs in the keyring.
     *
     * @return An unmodifiable set of the key identifiers in the keyring.
     */
    Set<String> list(User user) throws KeyringException;

    /**
     * Unlock the user keyring.
     *
     * <b>Note:</b> This is to maintain backwards compatibility only. This functionality is not required by the new
     * central keyring implementation.
     *
     * @param user     the user the keyring belongs to.
     * @param password the user's password.
     * @throws KeyringException problem unlocking the keyring.
     */
    void unlock(User user, String password) throws KeyringException;

    /**
     * Assign the List of keys to a user. Required to maintain backwards compatibility
     */
    void assignTo(User src, User target, List<CollectionDescription> assignments) throws KeyringException;

    /**
     * Assign the array of keys to a user. Required to maintain backwards compatibility
     */
    void assignTo(User src, User target, CollectionDescription... assignments) throws KeyringException;

    /**
     * Remove the List of keys from a user. Required to maintain backwards compatibility
     */
    void revokeFrom(User target, List<CollectionDescription> removals) throws KeyringException;

    /**
     * Remove the array of keys from a user. Required to maintain backwards compatibility
     */
    void revokeFrom(User target, CollectionDescription... removals) throws KeyringException;
}
