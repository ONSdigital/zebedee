package com.github.onsdigital.zebedee.dao;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.AdminOptions;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyManager;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.google.gson.JsonSyntaxException;
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
 *
 */
public class UsersDaoImpl implements UsersDao {

    private static UsersDao instance = null;

    private static final String JSON_EXT = ".json";
    private static final String SYSTEM_USER = "system";

    private final ReentrantLock lock = new ReentrantLock();
    private Path users;
    private Zebedee zebedee;

    // TODO Need to think about this need to implement a thread safe singleton.
    public static UsersDao init(Path users, Zebedee zebedee) {
        instance = new UsersDaoImpl(users, zebedee);
        return instance;
    }

    private UsersDaoImpl(Path users, Zebedee zebedee) {
        this.users = users;
        this.zebedee = zebedee;
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
        if (zebedee.getPermissions().hasAdministrator()) {
            logDebug(SYSTEM_USER_ALREADY_EXISTS_MSG).log();
            return;
        }

        // Create the user at a lower level because we don't have a Session at this point:
        create(user, SYSTEM_USER);
        write(resetPassword(user, password, SYSTEM_USER));
        zebedee.getPermissions().addEditor(user.email, null);
        zebedee.getPermissions().addAdministrator(user.email, null);
    }

    @Override
    public void createPublisher(User user, String password, Session session) throws IOException,
            UnauthorizedException, ConflictException, BadRequestException, NotFoundException {
        create(session, user);
        Credentials credentials = new Credentials();
        credentials.email = user.email;
        credentials.password = password;
        setPassword(session, credentials);
        zebedee.getPermissions().addEditor(user.email, session);
    }

    @Override
    public User create(Session session, User user) throws UnauthorizedException, IOException, ConflictException,
            BadRequestException {
        if (!zebedee.getPermissions().isAdministrator(session)) {
            throw new UnauthorizedException(CREATE_USER_AUTH_ERROR_MSG);
        }
        if (exists(user)) {
            throw new ConflictException(format(USER_ALREADY_EXISTS_MSG, user.email));
        }
        if (!valid(user)) {
            throw new BadRequestException(USER_DETAILS_INVALID_MSG);
        }
        return create(user, session.email);
    }

    @Override
    public boolean setPassword(Session session, Credentials credentials) throws IOException, UnauthorizedException,
            BadRequestException, NotFoundException {
        boolean isSuccess = false;

        if (session == null) {
            throw new UnauthorizedException("Not authenticated.");
        }
        if (credentials == null || !exists(credentials.email)) {
            throw new BadRequestException(USER_DETAILS_INVALID_MSG);
        }

        User user = read(credentials.email);
        if (!zebedee.getPermissions().isAdministrator(session) && !user.authenticate(credentials.oldPassword)) {
            throw new UnauthorizedException("Authentication failed with old password.");
        }

        lock.lock();
        try {
            boolean settingOwnPwd = credentials.email.equalsIgnoreCase(session.email)
                    && StringUtils.isNotBlank(credentials.password);

            if (settingOwnPwd) {
                isSuccess = changePassword(user, credentials.oldPassword, credentials.password);
            } else {
                // Only an admin can update another users password.
                if (zebedee.getPermissions().isAdministrator(session.email) || !zebedee.getPermissions()
                        .hasAdministrator()) {

                    // Administrator reset, or system setup Grab current keyring (null if this is system setup)
                    Keyring originalKeyring = null;
                    if (user.keyring != null) {
                        originalKeyring = user.keyring.clone();
                    }

                    user = resetPassword(user, credentials.password, session.email);
                    // Restore the user keyring (or not if this is system setup)
                    if (originalKeyring != null) {
                        KeyManager.transferKeyring(user.keyring, zebedee.getKeyringCache().get(session), originalKeyring.list());
                    }
                    write(user);
                    isSuccess = true;
                } else {
                    // Set password unsuccessful.
                    logInfo("Set password unsuccessful, only admin users can set another users password.")
                            .addParameter("callingUser", session.email)
                            .addParameter("targetedUser", credentials.email)
                            .log();
                }
            }
        } catch (Exception ex) {
            logError(ex).user(user.email).addMessage("Error while attempting to set user password").log();
        } finally {
            lock.unlock();
        }
        return isSuccess;
    }

    @Override
    public UserList list() throws IOException {
        UserList result = new UserList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(users)) {
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
                // TODO this could be moved to the collections class for reuse.
                Map<String, Collection> collectionMap = zebedee.getCollections()
                        .list()
                        .stream()
                        .collect(Collectors.toMap(collection -> collection.getDescription().getId(), collection -> collection));

                List<String> keysToRemove = user.keyring()
                        .list()
                        .stream()
                        .filter(key -> {
                            if (zebedee.getApplicationKeys().containsKey(key)) {
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
        if (!zebedee.getPermissions().isAdministrator(session.email)) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!exists(user.email)) {
            throw new NotFoundException("User " + user.email + " could not be found");
        }
        return update(user, updatedUser, session.email);
    }

    @Override
    public boolean delete(Session session, User user) throws IOException, UnauthorizedException, NotFoundException {
        if (zebedee.getPermissions().isAdministrator(session.email) == false) {
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

    User create(User user, String lastAdmin) throws IOException {
        User result = null;

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
            result = users.resolve(userFileName);
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
        boolean result = user.changePassword(oldPassword, newPassword);
        if (result) {
            lock.lock();
            try {
                // Make sure we have the latest before we save.
                user = read(user.email);
                user.inactive = false;
                user.lastAdmin = user.email;
                user.temporaryPassword = false;
                write(user);
            } finally {
                lock.unlock();
            }
        }
        return result;
    }

    private User resetPassword(User user, String password, String adminEmail) throws IOException {
        lock.lock();
        try {
            User current = read(user.email);
            current.resetPassword(password);
            current.inactive = false;
            current.lastAdmin = adminEmail;
            current.temporaryPassword = true;
            // Caller is responsible for write.
            //write(current);
            return current;
        } catch (IOException ex) {
            logError(ex).log();
            // TODO return a proper exception here.
            throw new RuntimeException(ex);
        } finally {
            lock.unlock();
        }
    }
}
