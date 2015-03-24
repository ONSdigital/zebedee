package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Password;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 12/03/2015.
 */
public class Users {
    private Path users;

    public Users(Path users) {
        this.users = users;
    }

    /**
     * Creates a user. This is used to create the initial administrator user when the system is set up.
     *
     * @param zebedee  A {@link Zebedee} instance.
     * @param user     The details of the {@link User} to be created.
     * @param password The plaintext password for this admin user.
     * @return The created user.
     * @throws IOException If a filesystem error occurs.
     */
    public static void createAdmin(Zebedee zebedee, User user, String password) throws IOException {
        user.passwordHash = Password.hash(password);
        zebedee.users.write(user);
        zebedee.permissions.addAdministrator(user.email);
        zebedee.permissions.addEditor(user.email);
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

    /**
     * Sets the specified user's password and sets the account to active.
     *
     * @param email    The user ID.
     * @param password The password to set.
     * @param session  The logged in session.
     * @return True if the password was set. If no user exists for the given email address, false.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean setPassword(String email, String password, Session session) throws IOException {
        boolean result = false;

        if (session != null) {
            // Check permissions - must be an administrator to set a password:
            boolean isAdministrator = Root.zebedee.permissions.isAdministrator(session.email);
            if (!isAdministrator) {
                return false;
            }
        }

        return setPassword(email, password);
    }

    /**
     * Sets the specified user's password and sets the account to active.
     *
     * @param email    The user ID.
     * @param password The password to set.
     * @return True if the password was set. If no user exists for the given email address, false.
     * @throws IOException If a filesystem error occurs.
     */
    private boolean setPassword(String email, String password) throws IOException {
        boolean result = false;

        User user = get(email);
        if (user != null) {
            user.passwordHash = Password.hash(password);
            user.inactive = false;
            write(user);
            result = true;
        }

        return result;
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
}
