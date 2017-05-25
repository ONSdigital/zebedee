package com.github.onsdigital.zebedee.service;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.AdminOptions;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.KeyManager;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.Permissions;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
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
    private static final String SYSTEM_USER = "system";

    private static UsersService INSTANCE = null;

    private final ReentrantLock lock = new ReentrantLock();

    private Path usersPath;
    private Permissions permissions;
    private ApplicationKeys applicationKeys;
    private KeyringCache keyringCache;
    private Collections collections;

    /**
     * Get a singleton instance of {@link UsersServiceImpl}.
     */
    public static UsersService getInstance(Path users, Collections collections, Permissions permissions,
                                           ApplicationKeys applicationKeys, KeyringCache keyringCache) {
        if (INSTANCE == null) {
            synchronized (MUTEX) {
                if (INSTANCE == null) {
                    INSTANCE = new UsersServiceImpl(users, collections, permissions, applicationKeys, keyringCache);
                }
            }
        }
        return INSTANCE;
    }

    /**
     *
     * @param usersPath
     * @param collections
     * @param permissions
     * @param applicationKeys
     * @param keyringCache
     */
    UsersServiceImpl(Path usersPath, Collections collections, Permissions permissions, ApplicationKeys
            applicationKeys, KeyringCache keyringCache) {
        this.usersPath = usersPath;
        this.permissions = permissions;
        this.applicationKeys = applicationKeys;
        this.collections = collections;
        this.keyringCache = keyringCache;
    }

    @Override
    public User addKeyToKeyring(String email, String keyIdentifier, SecretKey key) throws IOException {
        lock.lock();
        try {
            User user = read(email);
            user.keyring().put(keyIdentifier, key);
            write(user);
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

            if (!exists(email)) {
                throw new NotFoundException(format(UNKNOWN_USER_MSG, email));
            }
            return read(email);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean exists(String email) throws IOException {
        return StringUtils.isNotBlank(email) && Files.exists(userPath(email));
    }

    @Override
    public boolean exists(User user) throws IOException {
        return user != null && exists(user.email);
    }

    @Override
    public void createSystemUser(User user, String password) throws IOException, UnauthorizedException,
            NotFoundException, BadRequestException {
        if (permissions.hasAdministrator()) {
            logDebug(SYSTEM_USER_ALREADY_EXISTS_MSG).log();
            return;
        }

        // Create the user at a lower level because we don't have a Session at this point:
        user = create(user, SYSTEM_USER);
        write(resetPassword(user, password, SYSTEM_USER));
        permissions.addEditor(user.email, null);
        permissions.addAdministrator(user.email, null);
    }

    @Override
    public void createPublisher(User user, String password, Session session) throws IOException,
            UnauthorizedException, ConflictException, BadRequestException, NotFoundException {
        create(session, user);
        Credentials credentials = new Credentials();
        credentials.email = user.email;
        credentials.password = password;
        setPassword(session, credentials);
        permissions.addEditor(user.email, session);
    }

    @Override
    public User create(Session session, User user) throws UnauthorizedException, IOException, ConflictException,
            BadRequestException {
        if (!permissions.isAdministrator(session)) {
            throw new UnauthorizedException(CREATE_USER_AUTH_ERROR_MSG);
        }
        if (exists(user)) {
            throw new ConflictException(format(USER_ALREADY_EXISTS_MSG, user.email));
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
        if (!exists(credentials.email)) {
            throw new BadRequestException("Cannot set password as user does not exist");
        }

        lock.lock();
        try {
            User user = read(credentials.email);

            if (!permissions.isAdministrator(session) && !user.authenticate(credentials.oldPassword)) {
                throw new UnauthorizedException("Authentication failed with old password.");
            }

            boolean settingOwnPwd = credentials.email.equalsIgnoreCase(session.getEmail())
                    && StringUtils.isNotBlank(credentials.password);

            if (settingOwnPwd) {
                isSuccess = changePassword(user, credentials.oldPassword, credentials.password);
            } else {
                // Only an admin can update another users password.
                if (permissions.isAdministrator(session.getEmail()) || !permissions.hasAdministrator()) {

                    // Administrator reset, or system setup Grab current keyring (null if this is system setup)
                    Keyring originalKeyring = null;
                    if (user.keyring != null) {
                        originalKeyring = user.keyring.clone();
                    }

                    user = resetPassword(user, credentials.password, session.getEmail());
                    // Restore the user keyring (or not if this is system setup)
                    if (originalKeyring != null) {
                        KeyManager.transferKeyring(user.keyring, keyringCache.get(session), originalKeyring.list());
                    }
                    write(user);
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
        UserList result = new UserList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(usersPath)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    try (InputStream input = Files.newInputStream(path)) {
                        User user = Serialiser.deserialise(input, User.class);
                        result.add(user);
                    } catch (JsonSyntaxException e) {
                        logError(e, "Error deserialising user").addParameter("path", path.toString()).log();
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void removeStaleCollectionKeys(String userEmail) throws IOException, NotFoundException, BadRequestException {
        lock.lock();
        try {
            User user = getUserByEmail(userEmail);

            if (user.keyring != null) {
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
                    update(user, user, user.lastAdmin);
                }
            }
        } finally {
            lock.unlock();
        }
    }


    @Override
    public User removeKeyFromKeyring(String email, String keyIdentifier) throws IOException {
        lock.lock();
        try {
            User user = read(email);
            user.keyring().remove(keyIdentifier);
            write(user);
            return user;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public User update(Session session, User user, User updatedUser) throws IOException, UnauthorizedException,
            NotFoundException, BadRequestException {
        if (!permissions.isAdministrator(session.getEmail())) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!exists(user.email)) {
            throw new NotFoundException("User " + user.email + " could not be found");
        }
        return update(user, updatedUser, session.getEmail());
    }

    @Override
    public boolean delete(Session session, User user) throws IOException, UnauthorizedException, NotFoundException {
        if (permissions.isAdministrator(session.getEmail()) == false) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!exists(user.email)) {
            throw new NotFoundException(format(UNKNOWN_USER_MSG, user.email));
        }

        Path path = userPath(user.email);
        lock.lock();
        try {
            return Files.deleteIfExists(path);
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
            User updated = read(user.email);
            if (updated != null) {
                updated.keyring = user.keyring.clone();

                // Only set this to true if explicitly set:
                updated.inactive = BooleanUtils.isTrue(user.inactive);
                write(updated);
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
            if (valid(user) && !exists(user.email)) {
                result = new User();
                result.email = user.email;
                result.name = user.name;
                result.inactive = true;
                result.temporaryPassword = true;
                result.lastAdmin = lastAdmin;
                write(result);
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

                user = read(user.email);
                // The keyring has not been generated yet,
                // so reset the password to the current password
                // in order to generate a keyring and associated key pair:
                logDebug("Generating keyring").user(user.email).log();
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
                if (updatedUser.name != null && updatedUser.name.length() > 0) {
                    user.name = updatedUser.name;
                }
                // Create adminOptions object if user doesn't already have it
                if (user.adminOptions == null) {
                    user.adminOptions = new AdminOptions();
                }
                // Update adminOptions object if updatedUser options are different to stored user options
                if (updatedUser.adminOptions != null) {
                    if (updatedUser.adminOptions.rawJson != user.adminOptions.rawJson) {
                        user.adminOptions.rawJson = updatedUser.adminOptions.rawJson;
                        System.out.println(user.adminOptions.rawJson);
                    }
                }

                user.lastAdmin = lastAdmin;
                write(user);
            }
            return user;
        } finally {
            lock.unlock();
        }
    }

    private void write(User user) throws IOException {
        lock.lock();
        try {
            user.email = normalise(user.email);
            Path userPath = userPath(user.email);
            Serialiser.serialise(userPath, user);
        } catch (Exception ex) {
            logError(ex).log();
        } finally {
            lock.unlock();
        }
    }

    private User read(String email) throws IOException {
        lock.lock();
        try {
            User result = null;
            if (exists(email)) {
                result = Serialiser.deserialise(userPath(email), User.class);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    private Path userPath(String email) {
        Path result = null;
        if (StringUtils.isNotBlank(email)) {
            String userFileName = PathUtils.toFilename(normalise(email));
            userFileName += JSON_EXT;
            result = usersPath.resolve(userFileName);
        }
        return result;
    }

    private String normalise(String email) {
        return StringUtils.lowerCase(StringUtils.trim(email));
    }

    private boolean valid(User user) {
        return user != null && StringUtils.isNoneBlank(user.email, user.name);
    }

    private boolean changePassword(User user, String oldPassword, String newPassword) throws IOException {
        lock.lock();
        try {
            if (user.changePassword(oldPassword, newPassword)) {
/*                // Make sure we have the latest before we save.
                user = read(user.email);*/
                user.inactive = false;
                user.lastAdmin = user.email;
                user.temporaryPassword = false;
                write(user);
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
            user.inactive = false;
            user.lastAdmin = adminEmail;
            user.temporaryPassword = true;
            return user;
        } finally {
            lock.unlock();
        }
    }
}
