package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * This class duplicates the existing (legacy) {@link com.github.onsdigital.zebedee.json.Keyring} and {@link KeyringCache}
 * functionality behind a newly defined Keyring interface. Doing so allows to start the process of migrating to
 * the new {@link Keyring} abstraction while maintaing backwards compatability and the ability to swicth back if
 * necessary.
 */
public class LegacyKeyringImpl implements Keyring {

    static final String USER_NULL_ERR = "user required but was null";
    static final String EMAIL_EMPTY_ERR = "user email required but was empty";
    static final String USER_KEYRING_NULL_ERR = "user keyring expected was null";
    static final String GET_SESSION_ERR = "error getting user session";
    static final String CACHE_PUT_ERR = "error added user keys to legacy keyring cache";
    static final String COLLECTION_NULL_ERR = "collection required but was null";
    static final String COLLECTION_DESC_NULL_ERR = "collection description required but was null";
    static final String COLLECTION_ID_EMPTY_ERR = "collection ID required but was null/empty";
    static final String SECRET_KEY_NULL_ERR = "secret key required but was null";
    static final String CACHE_GET_ERR = "error getting entry from keyringCache";
    static final String SAVE_USER_ERR = "error persiting user update";
    static final String GET_USER_ERR = "error getting user";

    private Sessions sessions;
    private UsersService users;
    private KeyringCache cache;
    private ApplicationKeys applicationKeys;

    /**
     * Construct a new instance of the Legacy keyring.
     *
     * @param sessions        the {@link Sessions} service to use.
     * @param cache           the {@link KeyringCache} to use.
     * @param applicationKeys the {@link ApplicationKeys} to use.
     */
    public LegacyKeyringImpl(final Sessions sessions, final UsersService users, final KeyringCache cache,
                             final ApplicationKeys applicationKeys) {
        this.sessions = sessions;
        this.users = users;
        this.cache = cache;
        this.applicationKeys = applicationKeys;
    }

    /**
     * Add the user's keyring to the {@link KeyringCache}.
     *
     * @param user the user that populates the keyring.
     * @throws KeyringException thrown if the user was null, their email was null/empty or a session for the user
     *                          could not be found.
     */
    @Override
    public void cacheUserKeyring(User user) throws KeyringException {
        validateUser(user);
        Session session = getUserSession(user);

        if (session != null) {
            addUserKeyringToCache(user, session);
        }

        applicationKeys.populateCacheFromUserKeyring(user.keyring());
    }

    private Session getUserSession(User user) throws KeyringException {
        Session session = null;
        try {
            session = sessions.find(user.getEmail());
        } catch (IOException ex) {
            throw new KeyringException(GET_SESSION_ERR, ex);
        }

        return session;
    }

    private void addUserKeyringToCache(User user, Session session) throws KeyringException {
        try {
            cache.put(user, session);
        } catch (IOException ex) {
            throw new KeyringException(CACHE_PUT_ERR, ex);
        }
    }

    /**
     * Get the {@link SecretKey} for the specified collection. Returns the key if
     * <ul>
     *     <li>The users keyring exists in the {@link KeyringCache}.</li>
     *     <li>The cached {@link com.github.onsdigital.zebedee.json.Keyring} has been unlocked.</li>
     *     <li>The keyring contains the a key for the collection requested.</li>
     * </ul>
     *
     * @param user       the user requesting the key.
     * @param collection the collection to get the key for.
     * @return the {@link} if it exists (see conditions above).
     * @throws KeyringException thrown is the user is null, their email is null/empty, the collection is null, the
     *                          collection description is null the collection ID is null/empty, or if there is a problem getting the user
     *                          keyring from the cache.
     */
    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        validateUser(user);
        validateCollection(collection);

        com.github.onsdigital.zebedee.json.Keyring userKeyring = getCachedUserKeyring(user);
        if (userKeyring == null) {
            info().user(user.getEmail())
                    .collectionID(collection)
                    .log("get key unsuccessful user keyring not found in cache");
            return null;
        }

        if (!userKeyring.isUnlocked()) {
            info().user(user.getEmail())
                    .collectionID(collection)
                    .log("get key unsuccessful cached user keyring is locked");
            return null;
        }

        return userKeyring.get(collection.getDescription().getId());
    }

    /**
     * Remove the key for the specified collection from the user's keyring.
     * <ul>
     *     <li>Removes the key from the users cached keyring if it exists in {@link KeyringCache}</li>
     *     <li>Removed it from the stored user object (the user json file) and persists the change.</li>
     * </ul>
     *
     * @param user       the user performing the action.
     * @param collection the collection the the key belongs to.
     * @throws KeyringException thrown if the user is null, their email is null/empty, the collection is null, the
     *                          collection description is null, the collection ID is null/empty, there is a error
     *                          getting the users cached keyring or if there is an error updating the stored user.
     */
    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        validateUser(user);
        validateCollection(collection);

        // Remove the key from the cached user keyring
        com.github.onsdigital.zebedee.json.Keyring cachedKeyring = getCachedUserKeyring(user);
        if (cachedKeyring != null) {
            cachedKeyring.remove(collection.getDescription().getId());
        }

        // Remove the key from the user and save the changes.
        removeKeyFromStoredUser(user, collection);

    }

    /**
     * Add a key to the user's {@link com.github.onsdigital.zebedee.json.Keyring}.
     * <ul>
     *     <li>Add key to user's cached keyring if it exists in the {@link KeyringCache}.</li>
     *     <li>Add key to store user file and persists the change.</li>
     * </ul>
     *
     * @param user       the user adding the key.
     * @param collection the {@link Collection} the key belongs to.
     * @param key        the {@link SecretKey} to add.
     * @throws KeyringException thrown if the user is null, their email is null/empty, the collection is null, the
     *                          collection description is null, the collection ID is null/empty, the key is null, there
     *                          is a error updating the stored user file.
     */
    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {
        validateUser(user);
        validateCollection(collection);
        validateSecretKey(key);

        // Add key to users cached keyring if exists
        com.github.onsdigital.zebedee.json.Keyring cachedKeyring = getCachedUserKeyring(user);
        if (cachedKeyring != null) {
            cachedKeyring.put(collection.getDescription().getId(), key);
        }

        // Add the key to the stored user value.
        addKeyToStoredUser(user, collection, key);
    }

    /**
     * List the key held in the user's {@link com.github.onsdigital.zebedee.json.Keyring}.
     * <ul>
     *     <li>Lists from a cached user keyring if it exists in the {@link KeyringCache}.</li>
     *     <li>Otherwise lists from the stored user keyring.</li>
     *     <li>Returns an empty {@link Set} if the user's keyring is empty or null.</li>
     * </ul>
     *
     * @param user the user to list the keys for.
     * @return
     * @throws KeyringException thrown if the user is null, their email is null/empty, there is an error getting the
     *                          user's keyring from the cache or getting the stored user.
     */
    @Override
    public Set<String> list(User user) throws KeyringException {
        validateUser(user);

        com.github.onsdigital.zebedee.json.Keyring cachedKeyring = getCachedUserKeyring(user);
        if (cachedKeyring != null) {
            return cachedKeyring.list();
        }

        User storedUser = getUser(user);
        if (storedUser == null) {
            throw new KeyringException(USER_NULL_ERR);
        }

        if (storedUser.keyring() == null) {
            return new HashSet<>();
        }

        return storedUser.keyring().list();
    }

    private void validateUser(User user) throws KeyringException {
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

    private com.github.onsdigital.zebedee.json.Keyring getCachedUserKeyring(User user) throws KeyringException {
        try {
            return cache.get(user);
        } catch (IOException ex) {
            throw new KeyringException(CACHE_GET_ERR, ex);
        }
    }

    private void removeKeyFromStoredUser(User user, Collection collection) throws KeyringException {
        try {
            users.removeKeyFromKeyring(user.getEmail(), collection.getDescription().getId());
        } catch (IOException ex) {
            throw new KeyringException(SAVE_USER_ERR, ex);
        }
    }

    private void addKeyToStoredUser(User user, Collection collection, SecretKey key) throws KeyringException {
        try {
            users.addKeyToKeyring(user.getEmail(), collection.getDescription().getId(), key);
        } catch (IOException ex) {
            throw new KeyringException(SAVE_USER_ERR, ex);
        }
    }

    private User getUser(User user) throws KeyringException {
        try {
            return users.getUserByEmail(user.getEmail());
        } catch (Exception ex) {
            throw new KeyringException(GET_USER_ERR, ex);
        }
    }
}
