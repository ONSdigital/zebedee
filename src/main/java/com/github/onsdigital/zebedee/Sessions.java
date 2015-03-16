package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by david on 12/03/2015.
 */
public class Sessions {
    private Path sessions;

    public Sessions(Path sessions) {
        this.sessions = sessions;
    }

    /**
     * Creates a new session.
     *
     * @param email The email address of the user to create a session for
     * @return A session for the email. If one exists it returns that else it
     * creates a new one. If the supplied email is blank then null will
     * be returned.
     * @throws java.io.IOException If a filesystem error occurs.
     */
    public Session create(String email) throws IOException {
        Session result = null;

        if (StringUtils.isNotBlank(email)) {

            // Check for an existing session:
            result = find(email);

            // Otherwise go ahead and create
            if (result == null) {
                result = new Session();
                result.id = Random.id();
                result.email = email;
                write(result, result.id);
            }
        }

        return result;
    }

    /**
     * Gets the record for an existing session.
     *
     * @param id The session ID in order to locate the session record.
     * @return The requested session, unless the ID is blank or no record exists
     * for this ID.
     * @throws java.io.IOException If a filesystem error occurs.
     */
    public Session get(String id) throws IOException {
        Session result = null;

        // Check the session record exists:
        if (exists(id)) {
            // Deserialise the json to a session:
            Path sessionPath = sessionPath(id);
            result = read(id);
            updateLastAccessed(result);
        }

        return result;
    }

    /**
     * Locates a session from the user's email address (user ID).
     *
     * @param email The email.
     * @return An existing session, if found.
     * @throws IOException If a filesystem error occurs.
     */
    public Session find(String email) throws IOException {
        Session result = null;

        iterate:
        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(sessions)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {

                    // Examine each session for a match to the email address:
                    Session session = read(entry);
                    if (StringUtils.equalsIgnoreCase(session.email, email)
                            && valid(session)) {
                        result = session;
                    }

                    // Break out if we've found the session:
                    if (result != null) {
                        updateLastAccessed(session);
                        break iterate;
                    }
                }
            }
        }

        return result;
    }

    private boolean exists(String id) throws IOException {
        return StringUtils.isNotBlank(id) && Files.exists(sessionPath(id));
    }

    private Session updateLastAccessed(Session session) throws IOException {
        Session result = null;

        if (valid(session) && exists(session.id)) {

            result = read(session.id);
            result.lastAccess = new Date();
            write(session, result.id);
        }

        return result;
    }

    private boolean valid(Session session) {
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.MINUTE, -60);
        return session != null && StringUtils.isNotBlank(session.email)
                && !session.lastAccess.before(expiry.getTime());
    }

    private void write(Session session, String id) throws IOException {
        Path path = sessionPath(session.id);
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, session);
        }
    }

    private Session read(String id) throws IOException {
        return read(sessionPath(id));
    }

    private Session read(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return Serialiser.deserialise(input, Session.class);
        }
    }

    private Path sessionPath(String id) {
        Path result = null;

        if (StringUtils.isNotBlank(id)) {
            String sessionFileName = PathUtils.toFilename(id);
            sessionFileName += ".json";
            result = sessions.resolve(sessionFileName);
        }

        return result;
    }

    // TODO: Add an expiry method to delete old sessions.
}
