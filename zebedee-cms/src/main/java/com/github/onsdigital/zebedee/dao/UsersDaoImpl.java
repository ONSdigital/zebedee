package com.github.onsdigital.zebedee.dao;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.AdminOptions;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static java.text.MessageFormat.format;

public class UsersDaoImpl implements UsersDao {

    private static final String JSON_EXT = ".json";
    private static final String BLACK_EMAIL_MSG = "User email cannot be blank";
    private static final String UNKNOWN_USER_MSG = "User for email {0} not found";
    private static final String REMOVING_STALE_KEY_LOG_MSG = "Removing stale collection key from user.";

    private static UsersDao instance = null;

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
    public User getByEmail(String email) throws IOException, NotFoundException, BadRequestException {
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
    public void removeStaleCollectionKeys(String userEmail) throws IOException, NotFoundException, BadRequestException {
        lock.lock();
        try {
            User user = getByEmail(userEmail);

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
    public User addKeyToKeyring(String email, String keyIdentifier, SecretKey key) throws IOException {
        lock.lock();
        try {
            System.out.println("Adding key " + keyIdentifier + " to user " + email);
            User user = read(email);
            user.keyring().put(keyIdentifier, key);
            write(user);
            return user;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public User removeKeyFromKeyring(String email, String keyIdentifier) throws IOException {
        lock.lock();
        try {
            System.out.println("Removing key " + keyIdentifier + " from user " + email);
            User user = read(email);
            user.keyring().remove(keyIdentifier);
            write(user);
            return user;
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

}
