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

public class LegacyKeyringImpl implements Keyring {

    static final String USER_NULL_ERR = "user required but was null";
    static final String EMAIL_EMPTY_ERR = "user email required but was empty";
    static final String USER_KEYRING_NULL_ERR = "user keyring expected was null";
    static final String GET_SESSION_ERR = "error getting user session";
    static final String SESSION_NULL_ERR = "user session required but was null";
    static final String CACHE_PUT_ERR = "error added user keys to legacy keyring cache";

    private Sessions sessionsService;
    private KeyringCache cache;
    private ApplicationKeys applicationKeys;

    public LegacyKeyringImpl(final Sessions sessionsService, final KeyringCache cache, final ApplicationKeys applicationKeys) {
        this.sessionsService = sessionsService;
        this.cache = cache;
        this.applicationKeys = applicationKeys;
    }

    @Override
    public void populateFromUser(User user) throws KeyringException {
        validateUser(user);

        com.github.onsdigital.zebedee.json.Keyring userKeyring = getUserKeyring(user);
        Session session = getUserSession(user);

        try {
            cache.put(user, session);
        } catch (IOException ex) {
            throw new KeyringException(CACHE_PUT_ERR, ex);
        }

        applicationKeys.populateCacheFromUserKeyring(userKeyring);
    }

    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        return null;
    }

    @Override
    public void remove(User user, Collection collection) throws KeyringException {

    }

    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {

    }

    @Override
    public Set<String> list(User user) throws KeyringException {
        return null;
    }

    private void validateUser(User user) throws KeyringException {
        if (user == null) {
            throw new KeyringException(USER_NULL_ERR);
        }
    }

    private String getUserEmail(User user) throws KeyringException {
        if (StringUtils.isEmpty(user.getEmail())) {
            throw new KeyringException(EMAIL_EMPTY_ERR);
        }

        return user.getEmail();
    }

    private com.github.onsdigital.zebedee.json.Keyring getUserKeyring(User user) throws KeyringException {
        com.github.onsdigital.zebedee.json.Keyring userKeyring = user.keyring();

        if (userKeyring == null) {
            throw new KeyringException(USER_KEYRING_NULL_ERR);
        }

        return userKeyring;
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
