package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;

/**
 * KeyringMigrationImpl is an abstraction in front of the existing keyring funtionality. This abstraction enables us to
 * decouple the existing keyring functionality from the classes that use it. It also allows us to toggle between keyring
 * implementations (legacy or new centralised keyring) with minimum discruption to the classes that depend on it.
 * <p>
 * The intention is this class will initially use the existing keyring functionality. When the new central keyring
 * has been built we will dual run the old a new implementations - this allows us to migrate all keys to the new
 * keyring but also given us the option to rollback if needed. Once have confidence the new keyring is working the
 * legacy functionality can be remove without any impact on the classes depending on the keyring.
 * <p>
 * This is perhaps a slightly convoluted approach but it enables a smooth migrate to the keyring without breaking any
 * existing functionality.
 */
public class KeyringMigrationImpl implements Keyring {

    static final String USER_NULL_ERR = "user required but was null";
    static final String COLLECTION_NULL_ERR = "collection required but was null";
    static final String COLLECTION_DESC_NULL_ERR = "collection description required but was null";
    static final String COLLECTION_ID_EMPTY_ERR = "collection ID required but was null/empty";

    static final String USER_KEYRING_NULL_ERR = "user keyring expected was null";

    private Keyring centralKeyring;
    private boolean centralKeyringEnabled;

    /**
     * Construct a new instance of the the keyring.
     *
     * @param centralKeyringEnabled if true will use the new central keyring implementation, if false will use the
     *                              existing legacy keyring implementation
     * @param centralKeyring        the new central keyring implemnetation to use.
     */
    public KeyringMigrationImpl(boolean centralKeyringEnabled, Keyring centralKeyring) {
        this.centralKeyring = centralKeyring;
        this.centralKeyringEnabled = centralKeyringEnabled;
    }

    @Override
    public void populateFromUser(User user) throws KeyringException {
        // TODO
    }

    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        validate(user, collection);

        if (centralKeyringEnabled) {
            return null;
        }

        return getFromLegacyKeyring(user, collection);
    }

    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        validate(user, collection);

        if (centralKeyringEnabled) {
            return;
        }

        removeFromLegacyKeyring(user, collection);
    }

    private void removeFromLegacyKeyring(User user, Collection collection) throws KeyringException {
        com.github.onsdigital.zebedee.json.Keyring userKeyring = user.keyring();
        if (userKeyring == null) {
            throw new KeyringException(USER_KEYRING_NULL_ERR);
        }

        userKeyring.remove(collection.getDescription().getId());
    }

    private SecretKey getFromLegacyKeyring(User user, Collection collection) throws KeyringException {
        com.github.onsdigital.zebedee.json.Keyring userKeyring = user.keyring();
        if (userKeyring == null) {
            throw new KeyringException(USER_KEYRING_NULL_ERR);
        }

        return userKeyring.get(collection.getDescription().getId());
    }

    private void validate(User user, Collection collection) throws KeyringException {
        if (user == null) {
            throw new KeyringException(USER_NULL_ERR);
        }

        if (collection == null) {
            throw new KeyringException(COLLECTION_NULL_ERR);
        }

        if (collection.getDescription() == null) {
            throw new KeyringException(COLLECTION_DESC_NULL_ERR);
        }

        if (StringUtils.isEmpty(collection.getDescription().getId())) {
            throw new KeyringException(COLLECTION_ID_EMPTY_ERR);
        }
    }
}