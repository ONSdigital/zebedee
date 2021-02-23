package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;

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
    static final String SECRET_KEY_NULL_ERR = "secret key required but was null";
    static final String GET_SESSION_ERR = "error getting user session";
    static final String EMAIL_EMPTY_ERR = "user email required but was empty";
    static final String SESSION_NULL_ERR = "user session required but was null";
    static final String LEGACY_CACHE_ERR = "error populating legacy keyring cache from user";

    private boolean centralKeyringEnabled;
    private Keyring centralKeyring;
    private KeyringCache legacyKeyringCache;
    private ApplicationKeys applicationKeys;
    private Sessions sessionsService;

    /**
     * Construct a new instance of the the keyring.
     *
     * @param centralKeyringEnabled if true will use the new central keyring implementation, if false will use the
     *                              existing legacy keyring implementation
     * @param centralKeyring        the new central keyring implemnetation to use.
     */
    public KeyringMigrationImpl(boolean centralKeyringEnabled, Keyring centralKeyring, KeyringCache legacyKeyringCache,
                                ApplicationKeys applicationKeys, Sessions sessionsService) {
        this.centralKeyring = centralKeyring;
        this.centralKeyringEnabled = centralKeyringEnabled;
        this.legacyKeyringCache = legacyKeyringCache;
        this.applicationKeys = applicationKeys;
        this.sessionsService = sessionsService;
    }

    @Override
    public void populateFromUser(User user) throws KeyringException {
        validateUser(user);

        if (centralKeyringEnabled) {
            return;
        }

        populateLegacyKeyringCacheFromUser(user);
    }

    private void populateLegacyKeyringCacheFromUser(User user) throws KeyringException {
        com.github.onsdigital.zebedee.json.Keyring userKeyring = getUserKeyring(user);

        Session session = getUserSession(user);

        try {
            legacyKeyringCache.put(user, session);
        } catch (IOException ex) {
            throw new KeyringException(LEGACY_CACHE_ERR, ex);
        }

        applicationKeys.populateCacheFromUserKeyring(userKeyring);
    }

    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {
        validateUser(user);
        validateCollection(collection);
        validateSecretKey(key);

        if (centralKeyringEnabled) {
            return;
        }

        addKeyToLegacyKeyring(user, collection, key);
    }

    private void addKeyToLegacyKeyring(User user, Collection collection, SecretKey key) throws KeyringException {
        getUserKeyring(user).put(collection.getDescription().getId(), key);
    }

    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        validateUser(user);
        validateCollection(collection);

        if (centralKeyringEnabled) {
            return null;
        }

        return getFromLegacyKeyring(user, collection);
    }

    private SecretKey getFromLegacyKeyring(User user, Collection collection) throws KeyringException {
        return getUserKeyring(user).get(collection.getDescription().getId());
    }

    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        validateUser(user);
        validateCollection(collection);

        if (centralKeyringEnabled) {
            return;
        }

        removeFromLegacyKeyring(user, collection);
    }

    private void removeFromLegacyKeyring(User user, Collection collection) throws KeyringException {
        getUserKeyring(user).remove(collection.getDescription().getId());
    }

    private void validateUser(User user) throws KeyringException {
        if (user == null) {
            throw new KeyringException(USER_NULL_ERR);
        }
    }

    private void validateCollection(Collection collection) throws KeyringException {
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

    private void validateSecretKey(SecretKey key) throws KeyringException {
        if (key == null) {
            throw new KeyringException(SECRET_KEY_NULL_ERR);
        }
    }

    private com.github.onsdigital.zebedee.json.Keyring getUserKeyring(User user) throws KeyringException {
        com.github.onsdigital.zebedee.json.Keyring userKeyring = user.keyring();

        if (userKeyring == null) {
            throw new KeyringException(USER_KEYRING_NULL_ERR);
        }

        return userKeyring;
    }

    private String getUserEmail(User user) throws KeyringException {
        if (StringUtils.isEmpty(user.getEmail())) {
            throw new KeyringException(EMAIL_EMPTY_ERR);
        }

        return user.getEmail();
    }

    private Session getUserSession(User user) throws KeyringException {
        String email = getUserEmail(user);

        Session session = null;
        try {
            session = sessionsService.find(email);
        } catch (IOException ex) {
            throw new KeyringException(GET_SESSION_ERR, ex);
        }

        if (session == null) {
            throw new KeyringException(SESSION_NULL_ERR);
        }
        return session;
    }

}
