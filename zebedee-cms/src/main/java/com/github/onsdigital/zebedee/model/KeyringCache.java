package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.KeyNotFoundException;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides an basic in-memory cache for {@link Keyring} instances.
 */
@Deprecated
public class KeyringCache {

    // Publisher keyring keeps all available secret keys available
    private CollectionKeyCache schedulerCache;
    private Map<Session, Keyring> keyringMap;
    private Sessions sessions;

    @Deprecated
    public KeyringCache(Sessions sessions, CollectionKeyCache schedulerCache) {
        this.sessions = sessions;
        this.schedulerCache = schedulerCache;
        this.keyringMap = new ConcurrentHashMap<>();
    }

    /**
     * Stores the specified user's keyring, if unlocked, in the cache.
     *
     * @param user The user whose {@link Keyring} is to be stored.
     * @throws IOException If a general error occurs.
     */
    @Deprecated
    public void put(User user, Session session) throws IOException {
        if (user != null && user.keyring() != null && user.keyring().isUnlocked()) {
            if (session != null) {
                // add the keyring by session
                keyringMap.put(session, user.keyring());

                // populate the scheduler keyring
                for (String collectionId : user.keyring().list()) {
                    if (getKeyQuiet(collectionId) == null) {
                        schedulerCache.add(collectionId, user.keyring().get(collectionId));
                    }
                }
            }
        }
    }

    /**
     * Get the requested key from the cache.
     *
     * @param collectionID the collection ID of the key to get.
     * @return null if the key is not found or if {@link CollectionKeyCache#get(String)} throws a
     * {@link KeyNotFoundException}.
     * @throws KeyringException problem getting the key.
     */
    private SecretKey getKeyQuiet(String collectionID) throws KeyringException {
        SecretKey key = null;
        try {
            key = schedulerCache.get(collectionID);
        } catch (KeyNotFoundException ex) {
            // KeyNotFoundException means the requested key was not found in the cache. In this context that is valid
            // scenario so we swallow the exception and return null. Any other exception is an error and should be
            // thrown.
        }
        return key;
    }

    /**
     * Gets the specified user's keyring, if present in the cache.
     *
     * @param user The user whose {@link Keyring} is to be retrieved.
     * @return The {@link Keyring} if present, or null.
     * @throws IOException If a general error occurs.
     */
    @Deprecated
    public Keyring get(User user) throws IOException {
        Keyring result = null;

        if (user != null) {
            Session session = sessions.find(user.getEmail());
            if (session != null) {
                result = keyringMap.get(session);
            }
        }

        return result;
    }

    /**
     * Gets the specified session's keyring, if present in the cache.
     *
     * @param session The session whose {@link Keyring} is to be retrieved.
     * @return The {@link Keyring} if present, or null.
     * @throws IOException If a general error occurs.
     */
    @Deprecated
    public Keyring get(Session session) throws IOException {
        Keyring result = null;

        if (session != null) {
            result = keyringMap.get(session);
        }
        return result;
    }

    /**
     * Removes a keyring, if present, from the cache, based on an expired session.
     *
     * @param session The expired {@link Session} for which the {@link Keyring} is to be removed.
     * @throws IOException If a general error occurs.
     */
    @Deprecated
    public void remove(Session session) throws IOException {
        if (session != null) {
            keyringMap.remove(session);
        }
    }
}
