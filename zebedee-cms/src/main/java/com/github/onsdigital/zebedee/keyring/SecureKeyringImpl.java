package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.user.model.User;

/**
 * SecureKeyringImpl adds a permissions check wrapper around a {@link Keyring} instance to ensure only authorised
 * users can access collection encryption keys
 */
public class SecureKeyringImpl implements SecureKeyring {

    static final String USER_NULL_ERR = "user required but was null";
    static final String USER_KEYRING_NULL_ERR = "user keyring required but was null";
    static final String USER_KEYRING_LOCKED_ERR = "error user keyring is locked";

    private final Keyring centralKeyring;

    public SecureKeyringImpl(Keyring centralKeyring) {
        this.centralKeyring = centralKeyring;
    }

    @Override
    public void populateFromUser(User user) throws KeyringException {
        if (user == null) {
            throw new KeyringException(USER_NULL_ERR);
        }

        if (user.keyring() == null) {
            throw new KeyringException(USER_KEYRING_NULL_ERR);
        }

        if (!user.keyring().isUnlocked()) {
            throw new KeyringException(USER_KEYRING_LOCKED_ERR);
        }

        if (!user.keyring().list().isEmpty()) {
            for (String collectionID : user.keyring().list()) {
                centralKeyring.add(collectionID, user.keyring().get(collectionID));
            }
        }
    }
}
