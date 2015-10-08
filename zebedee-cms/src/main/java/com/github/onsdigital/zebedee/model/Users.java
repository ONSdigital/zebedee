package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Password;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 12/03/2015.
 * <p/>
 * Class to handle user management functions
 */
public class Users {
    private Path users;
    private Zebedee zebedee;

    public Users(Path users, Zebedee zebedee) {
        this.users = users;
        this.zebedee = zebedee;
    }

    /**
     * Creates a user.
     *
     * @param zebedee  A {@link Zebedee} instance.
     * @param user     The details of the {@link User} to be created.
     * @param password The plaintext password for this admin user.
     * @param session  An administrator session.
     * @throws IOException If a filesystem error occurs.
     */
    public static void createPublisher(Zebedee zebedee, User user, String password, Session session) throws IOException, UnauthorizedException {
        user.passwordHash = Password.hash(password);
        zebedee.users.write(user);
        zebedee.permissions.addEditor(user.email, session);
    }

    /**
     * Creates the initial system user.
     *
     * @param zebedee  A {@link Zebedee} instance.
     * @param user     The details of the system {@link User}.
     * @param password The plaintext password for the user.
     * @throws IOException If a filesystem error occurs.
     */
    public static void createSystemUser(Zebedee zebedee, User user, String password) throws IOException, UnauthorizedException {

        if (zebedee.permissions.hasAdministrator()) {
            return;
        }

        zebedee.users.create(user);
        zebedee.users.setPassword(user.email, password, null);
        zebedee.permissions.addEditor(user.email, null);
        zebedee.permissions.addAdministrator(user.email, null);
    }

    /**
     * Gets the record for an existing user.
     *
     * @param email The user's email in order to locate the user record.
     * @return The requested user, unless the email is blank or no record exists for this email.
     * @throws IOException If a filesystem error occurs.
     */
    public User get(String email) throws IOException {

        // Check the user record exists:
        if (!exists(email)) {
            return null;
        }

        // Now deserialise the json to a user:
        User user;
        Path userPath = userPath(email);
        try (InputStream input = Files.newInputStream(userPath)) {
            user = Serialiser.deserialise(input, User.class);
        }

        return user;
    }

    /**
     * Gets the record for an existing user to be used by api get requests
     *
     * @param session a user session
     * @param email the user email
     * @return
     * @throws IOException - if a filesystem error occurs
     * @throws NotFoundException - if the email cannot be found
     * @throws BadRequestException - if the email is left blank
     */
    public User get(Session session,
                    String email) throws IOException, NotFoundException, BadRequestException {

        // Check email isn't blank (though this should redirect to userlist)
        if (StringUtils.isBlank(email)) {
            throw new BadRequestException("User email cannot be blank");
        }

        // Check user exists
        if (!exists(email)) {
            throw new NotFoundException("User for email " + email + " not found");
        }

        // Now deserialise the json to a user:
        User user;
        Path userPath = userPath(email);
        try (InputStream input = Files.newInputStream(userPath)) {
            user = Serialiser.deserialise(input, User.class);
        }

        return user;
    }

    public UserList getUserList(Session session) throws IOException {
        return zebedee.users.list();
    }


    private User removePasswordHash(User user) {
        if (user != null) {
            // Blank out the password hash.
            // Not strictly necessary, but sensible.
            user.passwordHash = null;
        }
        return user;
    }

    /**
     * Creates a new user.
     *
     * @param user The specification for the new user to be created. The name and email will be used.
     * @return The newly created user, unless a user already exists, or the supplied {@link com.github.onsdigital.zebedee.json.User} is not valid.
     * @throws IOException If a filesystem error occurs.
     */
    public User create(User user) throws IOException {
        User result = null;

        if (valid(user) && !exists(user.email)) {

            result = new User();
            result.email = user.email;
            result.name = user.name;
            result.inactive = true;

            Path userPath = userPath(result.email);
            try (OutputStream output = Files.newOutputStream(userPath)) {
                Serialiser.serialise(output, result);
            }
        }

        return result;
    }

    /**
     * Creates a new user.
     *
     * @param user The specification for the new user to be created. The name and email will be used.
     * @return The newly created user, unless a user already exists, or the supplied {@link com.github.onsdigital.zebedee.json.User} is not valid.
     * @throws IOException If a filesystem error occurs.
     */
    public User create(Session session,
                       User user) throws UnauthorizedException, IOException, ConflictException, BadRequestException {

        // Check the user has create permissions
        if (!zebedee.permissions.isAdministrator(session)) {
            throw new UnauthorizedException("This account is not permitted to create users.");
        }

        if (zebedee.users.exists(user)) {
            throw new ConflictException("User " + user.email + " already exists");
        }

        if (!valid(user)) {
            throw new BadRequestException("Insufficient user details given");
        }

        User result = new User();
        result.email = user.email;
        result.name = user.name;
        result.inactive = true;
        result.temporaryPassword = true;
        result.lastAdmin = session.email;

        Path userPath = userPath(result.email);
        try (OutputStream output = Files.newOutputStream(userPath)) {
            Serialiser.serialise(output, result);
        }

        return result;
    }

    /**
     * Updates the specified {@link com.github.onsdigital.zebedee.json.User}.
     * NB this does not allow you to update the email address, because
     * that would entail renaming the Json file that contains the user record.
     *
     * @param user The user record to be updated
     * @return The updated record.
     * @throws IOException If a filesystem error occurs.
     */
    public User update(User user) throws IOException {
        User result = null;

        if (exists(user)) {

            result = get(user.email);
            if (StringUtils.isNotBlank(user.name))
                result.name = user.name;
            if (user.inactive != null)
                result.inactive = user.inactive;

            write(result);
        }

        return result;
    }

    /**
     * Update user details
     *
     * At present user email cannot be updated
     *
     * @param session
     * @param user - a user object with the new details
     * @return
     * @throws IOException
     * @throws UnauthorizedException - Session does not have update permissions
     * @throws NotFoundException - user account does not exist
     * @throws BadRequestException - problem with the update
     */
    public User update(Session session,
                       User user) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        if (zebedee.permissions.isAdministrator(session.email) == false) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!zebedee.users.exists(user)) {
            throw new NotFoundException("User " + user.email + " could not be updated");
        }

        User updated = null;
        user.lastAdmin = session.email;

        updated = update(user);

        // We'll allow changing the email at some point.
        // It entails renaming the json file and checking
        // that the new email doesn't already exist.
        if (updated == null) {
            throw new BadRequestException("Unknown bad request exception");
        }

        return updated;
    }

    /**
     * Delete a user account
     *
     * @param session - an admin user session
     * @param user - a user object to delete
     * @return
     * @throws UnauthorizedException
     * @throws IOException
     * @throws NotFoundException
     */
    public boolean delete(Session session, User user) throws IOException, UnauthorizedException, NotFoundException {

        if (zebedee.permissions.isAdministrator(session.email) == false) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!zebedee.users.exists(user)) {
            throw new NotFoundException("User " + user.email + " does not exist");
        }

        Path path = userPath(user.email);
        Files.deleteIfExists(path);

        return true;
    }

    /**
     * Determines whether the given {@link com.github.onsdigital.zebedee.json.User} exists.
     *
     * @param user Can be null.
     * @return If the given user can be mapped to a user record, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean exists(User user) throws IOException {
        return user != null && exists(user.email);
    }

    /**
     * Determines whether a {@link com.github.onsdigital.zebedee.json.User} record exists for the given email.
     *
     * @param email Can be null.
     * @return If the given email can be mapped to a user record, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean exists(String email) throws IOException {
        return StringUtils.isNotBlank(email) && Files.exists(userPath(email));
    }

    /**
     * Authenticates using the given email address and password.
     *
     * @param email    The user ID.
     * @param password The user's password.
     * @return If given email maps to a {@link com.github.onsdigital.zebedee.json.User} record
     * and the password validates against the stored hash, true.
     * @throws IOException
     */
    public boolean authenticate(String email, String password) throws IOException {
        boolean result = false;

        User user = get(email);
        if (user != null && Password.verify(password, user.passwordHash)) {
            result = true;
        }

        return result;
    }


    public boolean setPassword(Session session, Credentials credentials) throws IOException, UnauthorizedException, BadRequestException {

        // Passwords can be changed by ...
        boolean permissionToChange = false;
        if (!zebedee.permissions.hasAdministrator() ||                  // anyone if we are setting a brand new password
                zebedee.permissions.isAdministrator(session.email) ||   // an administrator
                credentials.email.equalsIgnoreCase(session.email)       // the user themselves
                ) {
            permissionToChange = true;
        }

        if (!permissionToChange) {
            throw new UnauthorizedException("Passwords must be changed by admins or own user");
        }

        // Check the request
        if (credentials == null || !zebedee.users.exists(credentials.email)) {
            throw new BadRequestException("Please provide credentials (email, password)");
        }

        boolean temporaryPassword = true;
        if (credentials.temporaryPassword != null) {
            temporaryPassword = BooleanUtils.toBoolean(credentials.temporaryPassword);
        }

        return setPassword(credentials.email, credentials.password, session.email, temporaryPassword);
    }

    /**
     * Sets the specified user's password and sets the account to active.
     *
     * If it is the user changing the password marks it as permanent
     * If an admin is setting marks the password as temporary
     *
     * @param email    The user ID.
     * @param password The password to set.
     * @return True if the password was set. If no user exists for the given email address, false.
     * @throws IOException If a filesystem error occurs.
     */
    private boolean setPassword(String email, String password, String adminEmail, boolean temporaryPassword) throws IOException {
        boolean result = false;

        User user = get(email);
        if (user != null) {
            user.passwordHash = Password.hash(password);
            user.inactive = false;

            // Temporary password
            if(email.equalsIgnoreCase(adminEmail)) {
                user.lastAdmin = email;
                user.temporaryPassword = false;
            } else {
                user.lastAdmin = adminEmail;
                user.temporaryPassword = temporaryPassword;
            }

            write(user);
            result = true;
        }

        return result;
    }

    private boolean setPassword(String email, String password, String adminEmail) throws IOException {
        return setPassword(email, password, adminEmail, true);
    }

    /**
     * Determines whether the given {@link com.github.onsdigital.zebedee.json.User} is valid.
     *
     * @param user The object to check.
     * @return If the user is not null and neither email nor name ar blank, true.
     */
    private boolean valid(User user) {
        return user != null && StringUtils.isNoneBlank(user.email, user.name);
    }

    /**
     * Writes a user record to disk.
     *
     * @param user The record to be written.
     * @throws IOException If a filesystem error occurs.
     */
    private void write(User user) throws IOException {
        user.email = normalise(user.email);
        Path userPath = userPath(user.email);
        try (OutputStream output = Files.newOutputStream(userPath)) {
            Serialiser.serialise(output, user);
        }
    }

    /**
     * Generates a {@link java.nio.file.Path} for the given email address.
     *
     * @param email The email address to generate a {@link java.nio.file.Path} for.
     * @return A {@link java.nio.file.Path} to the specified user record.
     */
    private Path userPath(String email) {
        Path result = null;

        if (StringUtils.isNotBlank(email)) {
            String userFileName = PathUtils.toFilename(normalise(email));
            userFileName += ".json";
            result = users.resolve(userFileName);
        }

        return result;
    }

    /**
     * @param email An email address to be standardised.
     * @return The given email, trimmed and lowercased.
     */
    private String normalise(String email) {
        return StringUtils.lowerCase(StringUtils.trim(email));
    }

    /**
     * Return a collection of all users registered in the system
     *
     * @return
     */
    public UserList list() throws IOException {
        UserList result = new UserList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(users)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    try (InputStream input = Files.newInputStream(path)) {
                        User user = Serialiser.deserialise(input, User.class);
                        result.add(user);
                    }
                }
            }
        }

        return result;
    }
}
