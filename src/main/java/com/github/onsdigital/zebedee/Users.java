package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Password;
import com.github.davidcarboni.restolino.json.Serialiser;
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

    public User update(User user) throws IOException {
        User result = null;

        if (exists(user.email)) {

            result = get(user.email);
            if (StringUtils.isNotBlank(user.name))
            result.name = user.name;
            if (user.inactive != null)
            result.inactive = user.inactive;

            write(result);
        }

        return result;
    }

    public boolean exists(User user) throws IOException {
        return user != null && exists(user.email);
    }

    public boolean exists(String email) throws IOException {
        return StringUtils.isNotBlank(email) && Files.exists(userPath(email));
    }

    public boolean authenticate(String email, String password) throws IOException {
        boolean result = false;

        User user = get(email);
        if (user != null && Password.verify(password, user.passwordHash)) {
            result = true;
        }

        return result;
    }

    public boolean setPassword(String email, String password) throws IOException {
        boolean result = false;

        User user = get(email);
        if (user != null) {
            user.passwordHash = Password.hash(password);
            write(user);
        }

        return result;
    }

    private Path userPath(String email) {
        Path result = null;

        if (StringUtils.isNotBlank(email)) {
            String userFileName = PathUtils.toFilename(email);
            userFileName += ".json";
            result = users.resolve(userFileName);
        }

        return result;
    }

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
        Path userPath = userPath(user.email);
        try (OutputStream output = Files.newOutputStream(userPath)) {
            Serialiser.serialise(output, user);
        }
    }
}
