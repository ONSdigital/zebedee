package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;

public interface CollectionKeyring {

    /**
     * Populate the Keyring from an unlocked {@link User#keyring()}
     *
     * @param user the user that populates the keyring.
     * @throws KeyringException problem populating the keyring.
     */
    void populateFromUser(User user) throws KeyringException;

    /**
     *
     * @param user
     * @param collection
     * @return
     * @throws KeyringException
     */
    SecretKey get(User user, Collection collection) throws KeyringException;
}
