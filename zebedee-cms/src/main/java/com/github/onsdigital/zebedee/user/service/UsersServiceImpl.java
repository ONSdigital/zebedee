package com.github.onsdigital.zebedee.user.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.AdminOptions;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.store.UserStore;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
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
@Deprecated
public class UsersServiceImpl implements UsersService {

    private static final Object MUTEX = new Object();
    private static final String JSON_EXT = ".json";
    static final String SYSTEM_USER = "system";

    private static UsersService INSTANCE = null;

    private final ReentrantLock lock = new ReentrantLock();

    private PermissionsService permissionsService;
    private Collections collections;
    private UserStore userStore;
    private UserFactory userFactory;

    /**
     * There is a circular dependency between the UserService and the new Keyring they are both needed by each others
     * constructors. To get around this we're using a supplier which returns the keying. This allows us to construct
     * a new instance of the UserService without the actual Keyring object. When the Keyring is needed the supplier
     * allows to lazy it load it on demand. Admittedly this is a bit of hack but:
     * - This is Zebedee.
     * - This was a quick & reasonably clean way to solve this issue.
     */
    private Supplier<CollectionKeyring> keyringSupplier;

    /**
     * Get a singleton instance of {@link UsersServiceImpl}.
     */
    public static UsersService getInstance(UserStore userStore, Collections collections,
                                           PermissionsService permissionsService, Supplier<CollectionKeyring> keyringSupplier) {
        if (INSTANCE == null) {
            synchronized (MUTEX) {
                if (INSTANCE == null) {
                    INSTANCE = new UsersServiceImpl(userStore, collections, permissionsService, keyringSupplier);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Create a new instance or the user service. Callers outside this package should use getInstance() to obtain the
     * singleton instance.
     */
    UsersServiceImpl(UserStore userStore, Collections collections, PermissionsService permissionsService,
                     Supplier<CollectionKeyring> keyringSupplier) {
        this.permissionsService = permissionsService;
        this.collections = collections;
        this.keyringSupplier = keyringSupplier;
        this.userStore = userStore;
        this.userFactory = new UserFactory();
    }

    @Override
    public User getUserByEmail(String email) throws IOException, NotFoundException, BadRequestException {
        lock.lock();
        try {
            if (StringUtils.isBlank(email)) {
                throw new BadRequestException(BLANK_EMAIL_MSG);
            }

            if (!userStore.exists(email)) {
                info().data("user", email).log("no user exists with the specified email");
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
    public void createSystemUser(User user, String password) throws IOException, UnauthorizedException,
            NotFoundException, BadRequestException {
        if (permissionsService.hasAdministrator()) {
            info().log(SYSTEM_USER_ALREADY_EXISTS_MSG);
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
            User targetUser = userStore.get(credentials.getEmail());

            if (!permissionsService.isAdministrator(session) && !targetUser.authenticate(credentials.getOldPassword())) {
                throw new UnauthorizedException("Authentication failed with old password.");
            }

            boolean settingOwnPwd = credentials.getEmail().equalsIgnoreCase(session.getEmail())
                    && StringUtils.isNotBlank(credentials.password);

            if (settingOwnPwd) {
                isSuccess = changePassword(targetUser, credentials.getOldPassword(), credentials.getPassword());
            } else {
                // Only an admin can update another users password.
                if (permissionsService.isAdministrator(session) || !permissionsService.hasAdministrator()) {

                    targetUser = resetPassword(targetUser, credentials.getPassword(), session.getEmail());
                    userStore.save(targetUser);
                    isSuccess = true;
                } else {
                    // Set password unsuccessful.
                    info().data("callingUser", session.getEmail())
                            .data("targetedUser", credentials.getEmail())
                            .log("Set password unsuccessful, only admin users can set another users password.");
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
    public User update(Session session, User user, User updatedUser) throws IOException, UnauthorizedException,
            NotFoundException, BadRequestException {
        if (!permissionsService.isAdministrator(session)) {
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
        if (!permissionsService.isAdministrator(session)) {
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
                        info().data("User.adminoptions.rawJson", user.getAdminOptions().rawJson).log("Update");
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
