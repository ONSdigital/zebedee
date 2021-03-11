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
import java.util.Set;

/**
 * This class duplicates the existing (legacy) keyring functionality behind a newly defined Keyring interface.
 * Moving the keyring functionality behind an interface allows to swap implemetations more easily.
 */
public class LegacyKeyringImpl implements Keyring {

    static final String USER_NULL_ERR = "user required but was null";
    static final String EMAIL_EMPTY_ERR = "user email required but was empty";
    static final String USER_KEYRING_NULL_ERR = "user keyring expected was null";
    static final String GET_SESSION_ERR = "error getting user session";
    static final String SESSION_NULL_ERR = "user session required but was null";
    static final String CACHE_PUT_ERR = "error added user keys to legacy keyring cache";
    static final String COLLECTION_NULL_ERR = "collection required but was null";
    static final String COLLECTION_DESC_NULL_ERR = "collection description required but was null";
    static final String COLLECTION_ID_EMPTY_ERR = "collection ID required but was null/empty";
    static final String SECRET_KEY_NULL_ERR = "secret key required but was null";

    private Sessions sessionsService;
    private KeyringCache cache;
    private ApplicationKeys applicationKeys;

    /**
     * COnstruct a new instance of the Legacy keyring.
     *
     * @param sessionsService the {@link Sessions} service to use.
     * @param cache           the {@link KeyringCache} to use.
     * @param applicationKeys the {@link ApplicationKeys} to use.
     */
    public LegacyKeyringImpl(final Sessions sessionsService, final KeyringCache cache, final ApplicationKeys applicationKeys) {
        this.sessionsService = sessionsService;
        this.cache = cache;
        this.applicationKeys = applicationKeys;
    }

    @Override
    public void populateFromUser(User user) throws KeyringException {
        validateUser(user);

        Session session = getUserSession(user);

        try {
            cache.put(user, session);
        } catch (IOException ex) {
            throw new KeyringException(CACHE_PUT_ERR, ex);
        }

        applicationKeys.populateCacheFromUserKeyring(user.keyring());
    }

    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        validateUser(user);
        validateCollection(collection);
        return user.keyring().get(collection.getDescription().getId());
    }

    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        validateUser(user);
        validateCollection(collection);
        user.keyring().remove(collection.getDescription().getId());
    }

    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {
        validateUser(user);
        validateCollection(collection);
        validateSecretKey(key);
    }

    @Override
    public Set<String> list(User user) throws KeyringException {
        // TODO
        return null;
    }

    private Session getUserSession(User user) throws KeyringException {
        Session session = null;
        try {
            session = sessionsService.find(user.getEmail());
        } catch (IOException ex) {
            throw new KeyringException(GET_SESSION_ERR, ex);
        }

        if (session == null) {
            throw new KeyringException(SESSION_NULL_ERR);
        }

        return session;
    }

    void validateUser(User user) throws KeyringException {
        if (user == null) {
            throw new KeyringException(USER_NULL_ERR);
        }

        if (StringUtils.isEmpty(user.getEmail())) {
            throw new KeyringException(EMAIL_EMPTY_ERR);
        }

        if (user.keyring() == null) {
            throw new KeyringException(USER_KEYRING_NULL_ERR);
        }
    }

    void validateCollection(Collection collection) throws KeyringException {
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

    void validateSecretKey(SecretKey key) throws KeyringException {
        if (key == null) {
            throw new KeyringException(SECRET_KEY_NULL_ERR);
        }
    }
}
