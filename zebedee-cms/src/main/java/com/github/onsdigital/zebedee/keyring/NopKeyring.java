package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;

public class NopKeyring implements Keyring {

    @Override
    public void populateFromUser(User user) throws KeyringException {
        // DO NOTHING.
    }

    /**
     * @param user
     * @param collection
     * @return
     * @throws KeyringException
     */
    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        return null;
    }
}
