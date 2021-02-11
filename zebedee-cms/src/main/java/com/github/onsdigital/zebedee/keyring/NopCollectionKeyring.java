package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.user.model.User;

public class NopCollectionKeyring implements CollectionKeyring {

    @Override
    public void populateFromUser(User user) throws KeyringException {
        // DO NOTHING.
    }
}
