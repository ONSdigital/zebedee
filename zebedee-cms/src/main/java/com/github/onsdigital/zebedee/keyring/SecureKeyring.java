package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.user.model.User;

public interface SecureKeyring {

    /**
     * Populate the Keyring from an unlocked {@link User#keyring()}
     *
     * @param user the user that populates the keyring.
     * @throws KeyringException problem populating the keyring.
     */
    void populateFromUser(User user) throws KeyringException;
}
