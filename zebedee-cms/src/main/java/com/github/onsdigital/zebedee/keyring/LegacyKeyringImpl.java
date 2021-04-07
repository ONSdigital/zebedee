package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.cache.SchedulerKeyCache;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    static final String ADD_KEY_SAVE_ERR = "user service add key to user returned an error";
    static final String REMOVE_KEY_SAVE_ERR = "user service remove key from user returned an error";
    static final String GET_USER_ERR = "user service get user by email return an error";
    static final String PASSWORD_EMPTY_ERR = "user password required but was null or empty";
    static final String UNLOCK_KEYRING_ERR = "error unlocking user keyring";
    static final String GET_KEY_RECIPIENTS_ERR = "error getting recipients for collection key";
    static final String LIST_USERS_ERR = "error listing all users";
    static final String CACHE_KEYRING_NULL_ERR = "expected cached keyring but was not found";
    static final String KEYRING_LOCKED_ERR = "cached keyring has not been unlocked";
    static final String SAVE_USER_KEYRING_ERR = "error saving changes to user keyring";

    private Sessions sessions;
    private UsersService usersService;
    private PermissionsService permissions;
    private KeyringCache cache;
    private SchedulerKeyCache schedulerKeyCache;
    private ApplicationKeys applicationKeys;

    /**
     * Construct a new instance of the Legacy keyring.
     *
     * @param sessions        the {@link Sessions} service to use.
     * @param cache           the {@link KeyringCache} to use.
     * @param applicationKeys the {@link ApplicationKeys} to use.
     */
    public LegacyKeyringImpl(final Sessions sessions, final UsersService usersService, PermissionsService permissions,
                             final KeyringCache cache, final SchedulerKeyCache schedulerKeyCache,
                             final ApplicationKeys applicationKeys) {
        this.sessions = sessions;
        this.usersService = usersService;
        this.permissions = permissions;
        this.cache = cache;
        this.schedulerKeyCache = schedulerKeyCache;
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
    public void cacheKeyring(User user) throws KeyringException {
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
     * Remove the key for the specified collection from all user keyrings (cached and stored).
     *
     * @param user       the user performing the action. Nullable and not required by this implementation.
     * @param collection the collection the the key belongs to.
     * @throws KeyringException thrown if the user is null, their email is null/empty, the collection is null, the
     *                          collection description is null, the collection ID is null/empty, there is a error
     *                          getting the users cached keyring or if there is an error updating the stored user.
     */
    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        validateCollection(collection);
        UserList userList = listUsers();

        if (userList == null) {
            return;
        }

        for (User u : userList) {
            removeKeyFromUser(u, collection);
        }
    }

    /**
     * Add the key to all users who have permission to access the collection.
     * <ul>
     *     <li>If a user's keying exists in the {@link KeyringCache} the new key is added.</li>
     *     <li>Otherwise Add key to store user file and persists the change.</li>
     * </ul>
     *
     * @param user       the user adding the key (not required by this implementation).
     * @param collection the {@link Collection} the key belongs to.
     * @param key        the {@link SecretKey} to add.
     * @throws KeyringException thrown the collection is null, the collection description is null, the collection ID
     *                          is null/empty, the key is null, there is a error updating the stored user file.
     */
    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {
        validateCollection(collection);
        validateSecretKey(key);

        schedulerKeyCache.add(collection.getDescription().getId(), key);

        List<User> assignments = getKeyRecipients(collection);
        List<User> removals = getKeyToRemoveFrom(collection, assignments);

        for (User recipent : assignments) {
            assignKeyToRecipient(recipent, collection, key);
        }

        for (User removeFrom : removals) {
            removeKeyFromUser(removeFrom, collection);
        }
    }

    private List<User> getKeyRecipients(Collection collection) throws KeyringException {
        List<User> recipients = null;
        try {
            recipients = permissions.getCollectionAccessMapping(collection);
        } catch (IOException ex) {
            throw new KeyringException(GET_KEY_RECIPIENTS_ERR, ex);
        }

        if (recipients == null) {
            recipients = new ArrayList<>();
        }

        return recipients;
    }

    private List<User> getKeyToRemoveFrom(Collection collection, List<User> recipients) throws KeyringException {
        UserList allUsers = listUsers();

        if (allUsers == null) {
            return new ArrayList<>();
        }

        return allUsers.stream()
                .filter(user -> !recipients.contains(user))
                .collect(Collectors.toList());
    }

    private UserList listUsers() throws KeyringException {
        try {
            return usersService.list();
        } catch (IOException ex) {
            throw new KeyringException(LIST_USERS_ERR, ex);
        }
    }

    private void assignKeyToRecipient(User user, Collection collection, SecretKey key) throws KeyringException {
        com.github.onsdigital.zebedee.json.Keyring cachedKeyring = getCachedUserKeyring(user);
        if (cachedKeyring != null) {
            cachedKeyring.put(collection.getDescription().getId(), key);
        }

        try {
            usersService.addKeyToKeyring(user.getEmail(), collection.getDescription().getId(), key);
        } catch (IOException ex) {
            throw new KeyringException(ADD_KEY_SAVE_ERR, ex);
        }
    }

    private void removeKeyFromUser(User user, Collection collection) throws KeyringException {
        com.github.onsdigital.zebedee.json.Keyring cachedKeyring = getCachedUserKeyring(user);
        if (cachedKeyring != null) {
            cachedKeyring.remove(collection.getDescription().getId());
        }

        try {
            usersService.removeKeyFromKeyring(user.getEmail(), collection.getDescription().getId());
        } catch (IOException ex) {
            throw new KeyringException(REMOVE_KEY_SAVE_ERR, ex);
        }
    }

    /**
     * List the keys held in the user's {@link com.github.onsdigital.zebedee.json.Keyring}.
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

    @Override
    public void unlock(User user, String password) throws KeyringException {
        validateUser(user);
        validatePassword(password);

        if (!user.keyring().unlock(password)) {
            throw new KeyringException(UNLOCK_KEYRING_ERR);
        }
    }

    @Override
    public void populate(User src, User target, Set<String> collectionIDs) throws KeyringException {
        validateUser(src);

        com.github.onsdigital.zebedee.json.Keyring srcKeyring = getCachedUserKeyring(src);
        if (srcKeyring == null) {
            throw new KeyringException(CACHE_KEYRING_NULL_ERR);
        }

        if (!srcKeyring.isUnlocked()) {
            throw new KeyringException(KEYRING_LOCKED_ERR);
        }

        validateUser(target);

        if (collectionIDs == null || collectionIDs.isEmpty()) {
            return;
        }

        for (String id : collectionIDs) {
            SecretKey key = srcKeyring.get(id);
            if (key != null) {
                target.keyring().put(id, key);
            }
        }

        saveKeyringChanges(target);
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

    private User getUser(User user) throws KeyringException {
        try {
            return usersService.getUserByEmail(user.getEmail());
        } catch (Exception ex) {
            throw new KeyringException(GET_USER_ERR, ex);
        }
    }

    private void validatePassword(String password) throws KeyringException {
        if (StringUtils.isEmpty(password)) {
            throw new KeyringException(PASSWORD_EMPTY_ERR);
        }
    }

    private void saveKeyringChanges(User user) throws KeyringException {
        try {
            usersService.updateKeyring(user);
        } catch (IOException ex) {
            throw new KeyringException(SAVE_USER_KEYRING_ERR, ex);
        }
    }
}
