package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;

import javax.crypto.SecretKey;
import java.util.Set;

public interface CollectionKeyring {

    /**
     * Get a key from the keyring for the specified collection.
     *
     * @param session    the session requesting the key.
     * @param collection the collection to get the key for.
     * @return the collection key if it exists null if not.
     * @throws KeyringException problem getting the key.
     */
    SecretKey get(Session session, Collection collection) throws KeyringException;

    /**
     * Remove a key from the keyring.
     *
     * @param session    the session performing the action.
     * @param collection the collection the the key belongs to.
     * @throws KeyringException problem removing the key.
     */
    void remove(Session session, Collection collection) throws KeyringException;

    /**
     * Add a key to the keyring.
     *
     * @param session    the session of the user adding the key.
     * @param collection the {@link Collection} the key belongs to.
     * @param key        the {@link SecretKey} to add.
     * @throws KeyringException problem adding the key to the keyring.
     */
    void add(Session session, Collection collection, SecretKey key) throws KeyringException;

    /**
     * Lists the collection IDs in the keyring.
     *
     * @param session The session of the user making the request.
     * @return An unmodifiable set of the key identifiers in the keyring.
     */
    Set<String> list(Session session) throws KeyringException;

}
