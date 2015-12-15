package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides an basic in-memory cache for {@link Keyring} instances.
 */
public class KeyringCache {

    // Publisher keyring keeps all available secret keys available
    public Map<String, SecretKey> schedulerCache = new ConcurrentHashMap<>();
    private Map<Session, Keyring> keyringMap = new ConcurrentHashMap<>();
    private Zebedee zebedee;

    public KeyringCache(Zebedee zebedee) {

        this.zebedee = zebedee;
    }

    /**
     * Stores the specified user's keyring, if unlocked, in the cache.
     *
     * @param user The user whose {@link Keyring} is to be stored.
     * @throws IOException If a general error occurs.
     */
    public void put(User user, Session session) throws IOException {
        if (user != null && user.keyring() != null && user.keyring().isUnlocked()) {
            if (session != null) {
                // add the keyring by session
                keyringMap.put(session, user.keyring());

                // populate the scheduler keyring
                for (String collectionId : user.keyring.list()) {
                    if (!schedulerCache.containsKey(collectionId))
                        schedulerCache.put(collectionId, user.keyring.get(collectionId));
                }
            }
        }
    }

    /**
     * Gets the specified user's keyring, if present in the cache.
     *
     * @param user The user whose {@link Keyring} is to be retrieved.
     * @return The {@link Keyring} if present, or null.
     * @throws IOException If a general error occurs.
     */
    public Keyring get(User user) throws IOException {
        Keyring result = null;

        if (user != null) {
            Session session = zebedee.sessions.find(user.email);
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
    public void remove(Session session) throws IOException {
        if (session != null) {
            keyringMap.remove(session);
        }
    }
}
