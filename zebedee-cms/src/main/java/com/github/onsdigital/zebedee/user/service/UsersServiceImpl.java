package com.github.onsdigital.zebedee.user.service;

import com.github.onsdigital.zebedee.KeyManangerUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.AdminOptions;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.store.UserStore;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static java.text.MessageFormat.format;

/**
 * File system implementation of {@link UsersService} replacing legacy implmentation
 * {@link com.github.onsdigital.zebedee.model.Users}. This implemention uses a {@link ReentrantLock} to ensure that
 * only the thread which currently has the lock can execute a method that will modify a user file. The previous
 * implementation allowed 2 or more threads to potentially read the same user into memory, apply different in memory
 * updates before
 * writing the user back to the File system. In such cases the version of the user version written first would be
 * overwridden by subsequent updates - leading to data missing from user.<br/> The obvious performance implications
 * are outweighed by the correctness of data. The long term plan is to use a database.
 */
public class UsersServiceImpl implements UsersService {

    private static final Object MUTEX = new Object();
    private static final String JSON_EXT = ".json";
    static final String SYSTEM_USER = "system";

    private static UsersService INSTANCE = null;

    private final ReentrantLock lock = new ReentrantLock();

    // private Path users;
    private PermissionsService permissionsService;
    private KeyManangerUtil keyManangerUtil;
    private PermissionsService permissions;
    private ApplicationKeys applicationKeys;
    private KeyringCache keyringCache;
    private Collections collections;
    private UserStore userStore;
    private UserFactory userFactory;

    /**
     * Get a singleton instance of {@link UsersServiceImpl}.
     */
    public static UsersService getInstance(UserStore userStore, Collections collections,
                                           PermissionsService permissionsService, ApplicationKeys applicationKeys,
                                           KeyringCache keyringCache) {
        if (INSTANCE == null) {
            synchronized (MUTEX) {
                if (INSTANCE == null) {
                    INSTANCE = new UsersServiceImpl(userStore, collections, permissionsService, applicationKeys,
                            keyringCache);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Create a new instance. Callers from outside of this package should use
     * {@link UsersServiceImpl#getInstance(Collections, Permissions, ApplicationKeys, KeyringCache, UserStore)} to
     * obatin a singleton instance of this service.
     *
     * @param users
     * @param collections
     * @param permissionsServiceImpl
     * @param applicationKeys
     * @param keyringCache
     */
    UsersServiceImpl(UserStore userStore, Collections collections, PermissionsService permissionsService,
                     ApplicationKeys applicationKeys, KeyringCache keyringCache) {
        this.permissionsService = permissionsService;
        this.applicationKeys = applicationKeys;
        this.collections = collections;
        this.keyringCache = keyringCache;
        this.userStore = userStore;
        this.userFactory = new UserFactory();
        this.keyManangerUtil = new KeyManangerUtil();
    }

    @Override
    public User addKeyToKeyring(String email, String keyIdentifier, SecretKey key) throws IOException {
        lock.lock();
        try {
            User user = userStore.get(email);
            user.keyring().put(keyIdentifier, key);
            userStore.save(user);
            return user;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public User getUserByEmail(String email) throws IOException, NotFoundException, BadRequestException {
        lock.lock();
        try {
            if (StringUtils.isBlank(email)) {
                throw new BadRequestException(BLACK_EMAIL_MSG);
            }

            if (!userStore.exists(email)) {
                throw new NotFoundException(format(UNKNOWN_USER_MSG, email));
            }
            return userStore.get(email);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean exists(String email) throws IOException {
        return userStore.exists(email);
    }

    @Override
    public boolean exists(User user) throws IOException {
        return user != null && userStore.exists(user.getEmail());
    }

    @Override
    public void createSystemUser(User user, String password) throws IOException, UnauthorizedException,
            NotFoundException, BadRequestException {
        if (permissionsService.hasAdministrator()) {
            logDebug(SYSTEM_USER_ALREADY_EXISTS_MSG).log();
            return;
        }

        // Create the user at a lower level because we don't have a Session at this point:
        lock.lock();
        try {
            user = create(user, SYSTEM_USER);
            userStore.save(resetPassword(user, password, SYSTEM_USER));
            permissionsService.addEditor(user.getEmail(), null);
            permissionsService.addAdministrator(user.getEmail(), null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void createPublisher(User user, String password, Session session) throws IOException,
            UnauthorizedException, ConflictException, BadRequestException, NotFoundException {
        create(session, user);
        Credentials credentials = new Credentials();
        credentials.setEmail(user.getEmail());
        credentials.setPassword(password);
        setPassword(session, credentials);
        permissionsService.addEditor(user.getEmail(), session);
    }

    @Override
    public User create(Session session, User user) throws UnauthorizedException, IOException, ConflictException,
            BadRequestException {
        if (!permissionsService.isAdministrator(session)) {
            throw new UnauthorizedException(CREATE_USER_AUTH_ERROR_MSG);
        }
        if (user == null) {
            throw new BadRequestException(USER_IS_NULL_MSG);
        }
        if (userStore.exists(user.getEmail())) {
            throw new ConflictException(format(USER_ALREADY_EXISTS_MSG, user.getEmail()));
        }
        if (!valid(user)) {
            throw new BadRequestException(USER_DETAILS_INVALID_MSG);
        }
        return create(user, session.getEmail());
    }

    @Override
    public boolean setPassword(Session session, Credentials credentials) throws IOException, UnauthorizedException,
            BadRequestException, NotFoundException {
        boolean isSuccess = false;

        if (session == null) {
            new UnauthorizedException("Cannot set password as user is not authenticated.");
        }
        if (credentials == null) {
            throw new BadRequestException("Cannot set password for user as credentials is null.");
        }
        if (!userStore.exists(credentials.getEmail())) {
            throw new BadRequestException("Cannot set password as user does not exist");
        }

        lock.lock();
        try {
            User user = userStore.get(credentials.getEmail());

            if (!permissionsService.isAdministrator(session) && !user.authenticate(credentials.getOldPassword())) {
                throw new UnauthorizedException("Authentication failed with old password.");
            }

            boolean settingOwnPwd = credentials.getEmail().equalsIgnoreCase(session.getEmail())
                    && StringUtils.isNotBlank(credentials.password);

            if (settingOwnPwd) {
                isSuccess = changePassword(user, credentials.getOldPassword(), credentials.getPassword());
            } else {
                // Only an admin can update another users password.
                if (permissionsService.isAdministrator(session.getEmail()) || !permissionsService.hasAdministrator()) {

                    // Administrator reset, or system setup Grab current keyring (null if this is system setup)
                    Keyring originalKeyring = null;
                    if (user.keyring() != null) {
                        originalKeyring = user.keyring().clone();
                    }

                    user = resetPassword(user, credentials.getPassword(), session.getEmail());
                    // Restore the user keyring (or not if this is system setup)
                    if (originalKeyring != null) {
                        keyManangerUtil.transferKeyring(user.keyring(), keyringCache.get(session), originalKeyring.list());
                    }
                    userStore.save(user);
                    isSuccess = true;
                } else {
                    // Set password unsuccessful.
                    logInfo("Set password unsuccessful, only admin users can set another users password.")
                            .addParameter("callingUser", session.getEmail())
                            .addParameter("targetedUser", credentials.email)
                            .log();
                }
            }
            return isSuccess;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public UserList list() throws IOException {
        return userStore.list();
    }

    @Override
    public void removeStaleCollectionKeys(String userEmail) throws
            IOException, NotFoundException, BadRequestException {
        lock.lock();
        try {
            User user = getUserByEmail(userEmail);

            if (user.keyring() != null) {
                Map<String, Collection> collectionMap = collections.mapByID();

                List<String> keysToRemove = user.keyring()
                        .list()
                        .stream()
                        .filter(key -> {
                            if (applicationKeys.containsKey(key)) {
                                return false;
                            }
                            return !collectionMap.containsKey(key);
                        }).collect(Collectors.toList());

                keysToRemove.stream().forEach(staleKey -> {
                    logDebug(REMOVING_STALE_KEY_LOG_MSG)
                            .user(userEmail)
                            .collectionId(staleKey)
                            .log();
                    user.keyring().remove(staleKey);
                });

                if (!keysToRemove.isEmpty()) {
                    update(user, user, user.getLastAdmin());
                }
            }
        } finally {
            lock.unlock();
        }
    }


    @Override
    public User removeKeyFromKeyring(String email, String keyIdentifier) throws IOException {
        // TODO MIGHT WANT TO CONSIDER HOW WE MIGHT ROLLBACK IS THE SAVE CALL FAILS.
        lock.lock();
        try {
            User user = userStore.get(email);
            user.keyring().remove(keyIdentifier);
            userStore.save(user);
            return user;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public User update(Session session, User user, User updatedUser) throws IOException, UnauthorizedException,
            NotFoundException, BadRequestException {
        if (!permissionsService.isAdministrator(session.getEmail())) {
            throw new UnauthorizedException("Administrator permissionsServiceImpl required");
        }

        if (!userStore.exists(user.getEmail())) {
            throw new NotFoundException("User " + user.getEmail() + " could not be found");
        }
        return update(user, updatedUser, session.getEmail());
    }

    @Override
    public boolean delete(Session session, User user) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        if (session == null) {
            throw new BadRequestException("A session is required to delete a user.");
        }
        if (permissionsService.isAdministrator(session.getEmail()) == false) {
            throw new UnauthorizedException("Administrator permissionsServiceImpl required");
        }

        if (!userStore.exists(user.getEmail())) {
            throw new NotFoundException(format(UNKNOWN_USER_MSG, user.getEmail()));
        }

        lock.lock();
        try {
            return userStore.delete(user);
        } finally {
            lock.unlock();
        }
    }

    // TODO don't think this is required anymore.
    @Override
    public void migrateToEncryption(User user, String password) throws IOException {

        // Update this user if necessary:
        migrateUserToEncryption(user, password);

        int withKeyring = 0;
        int withoutKeyring = 0;
        // TODO Was listAll
        UserList users = list();
        for (User otherUser : users) {
            if (user.keyring() != null) {
                withKeyring++;
            } else {
                // Migrate test users automatically:
                if (migrateUserToEncryption(otherUser, "Dou4gl") || migrateUserToEncryption(otherUser, "password"))
                    withKeyring++;
                else
                    withoutKeyring++;
            }
        }

        logDebug("User info")
                .addParameter("numberOfUsers", users.size())
                .addParameter("withKeyRing", withKeyring)
                .addParameter("withoutKeyRing", withoutKeyring).log();
    }

    @Override
    public User updateKeyring(User user) throws IOException {
        lock.lock();
        try {
            User updated = userStore.get(user.getEmail());
            if (updated != null) {

                if (updated.keyring() == null) {
                    logDebug("User keyring not updated as it is currently null.")
                            .user(updated.getEmail())
                            .log();
                    return updated;
                }

                updated.setKeyring(user.keyring().clone());

                // Only set this to true if explicitly set:
                updated.setInactive(BooleanUtils.isTrue(user.getInactive()));
                userStore.save(updated);
            }
            return updated;
        } finally {
            lock.unlock();
        }
    }

    User create(User user, String lastAdmin) throws IOException {
        User result = null;
        lock.lock();
        try {
            if (valid(user) && !userStore.exists(user.getEmail())) {
                result = userFactory.newUserWithDefaultSettings(user.getEmail(), user.getName(), lastAdmin);
                userStore.save(result);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    private boolean migrateUserToEncryption(User user, String password) throws IOException {
        boolean result = false;
        lock.lock();
        try {
            if (user.keyring() == null && user.authenticate(password)) {

                System.out.println("\nUSER KEYRING IS NULL GENERATING NEW KEYRING\n");

                user = userStore.get(user.getEmail());
                // The keyring has not been generated yet,
                // so reset the password to the current password
                // in order to generate a keyring and associated key pair:
                logDebug("Generating keyring").user(user.getEmail()).log();
                user.resetPassword(password);
                update(user, user, "Encryption migration");
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    private User update(User user, User updatedUser, String lastAdmin) throws IOException {
        lock.lock();
        try {
            if (user != null) {
                if (updatedUser.getName() != null && updatedUser.getName().length() > 0) {
                    user.setName(updatedUser.getName());
                }
                // Create adminOptions object if user doesn't already have it
                if (user.getAdminOptions() == null) {
                    user.setAdminOptions(new AdminOptions());
                }
                // Update adminOptions object if updatedUser options are different to stored user options
                if (updatedUser.getAdminOptions() != null) {
                    if (updatedUser.getAdminOptions().rawJson != user.getAdminOptions().rawJson) {
                        user.getAdminOptions().rawJson = updatedUser.getAdminOptions().rawJson;
                        logDebug("Update").addParameter("User.adminoptions.rawJson", user.getAdminOptions().rawJson);
                    }
                }

                user.setLastAdmin(lastAdmin);
                userStore.save(user);
            }
            return user;
        } finally {
            lock.unlock();
        }
    }

    private String normalise(String email) {
        return StringUtils.lowerCase(StringUtils.trim(email));
    }

    private boolean valid(User user) {
        return user != null && StringUtils.isNoneBlank(user.getEmail(), user.getName());
    }

    private boolean changePassword(User user, String oldPassword, String newPassword) throws IOException {
        lock.lock();
        try {
            if (user.changePassword(oldPassword, newPassword)) {
                user.setInactive(false);
                user.setLastAdmin(user.getEmail());
                user.setTemporaryPassword(false);
                userStore.save(user);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * CALLER IS RESPONSIBLE FOR PERSISTING THIS.
     *
     * @param user
     * @param password
     * @param adminEmail
     * @return
     * @throws IOException
     */
    private User resetPassword(User user, String password, String adminEmail) throws IOException {
        lock.lock();
        try {
            user.resetPassword(password);
            user.setInactive(false);
            user.setLastAdmin(adminEmail);
            user.setTemporaryPassword(true);
            return user;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Factory class encapsulating the creation of new {@link User}.
     */
    protected class UserFactory {

        /**
         * Encapsulate the creation of a new {@link User} with default settings.
         * <ul>
         * <li>{@link User#email} -> email provided</li>
         * <li>{@link User#name} -> name provided</li>
         * <li>{@link User#inactive} -> true</li>
         * <li>{@link User#temporaryPassword} -> true</li>
         * <li>{@link User#keyring} -> null</li>
         * <li>{@link User#passwordHash} -> null</li>
         * </ul>
         *
         * @param email     the email address to set for the new user.
         * @param name      the name to set for the new user.
         * @param lastAdmin email / name of the last admin user to change / update this user.
         * @return a new {@link User} with the properties set as described above.
         */
        public User newUserWithDefaultSettings(String email, String name, String lastAdmin) {
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setInactive(true);
            user.setTemporaryPassword(true);
            user.setLastAdmin(lastAdmin);
            return user;
        }
    }
}
