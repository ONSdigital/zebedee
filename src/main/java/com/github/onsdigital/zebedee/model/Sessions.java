package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by david on 12/03/2015.
 */
public class Sessions extends TimerTask {

    private static final String tokenHeader = "x-florence-token";

    private Path sessions;
    Timer timer;

    public Sessions(Path sessions) {
        this.sessions = sessions;

        // Run every minute after the first minute:
        timer = new Timer();
        timer.schedule(this, 60 * 1000, 60 * 1000);
    }

    /**
     * This is the entrypoint for {@link java.util.TimerTask}.
     */
    @Override
    public void run() {
        try {
            deleteExpiredSessions();
        } catch (IOException e) {
            // Not much we can do.
            // This could periodically spam the logs
            // so need to review at some point.
            e.printStackTrace();
        }
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
        Session session = null;

        if (StringUtils.isNotBlank(email)) {

            // Check for an existing session:
            session = find(email);

            // Otherwise go ahead and create
            if (session == null) {
                session = new Session();
                session.id = Random.id();
                session.email = email;
                write(session);
            }
        }

        return session;
    }

    /**
     * Gets the record for an existing session.
     *
     * @param request The {@link HttpServletRequest}. The session ID will be retrieved from the {@value #tokenHeader} header.
     * @return The requested session, unless the ID is blank or no record exists
     * for this ID.
     * @throws java.io.IOException If a filesystem error occurs.
     */
    public Session get(HttpServletRequest request) throws IOException {
        String token = request.getHeader(tokenHeader);
        return get(token);
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
            // Deserialise the json:
            Session session = read(id);
            if (!expired(session)) {
                updateLastAccess(session);
                result = session;
            }
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
        Session session = null;

        // Find the session we're looking for:
        iterate:
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessions)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {

                    // Examine each session for a match to the email address:
                    Session candidate = read(entry);
                    if (StringUtils.equalsIgnoreCase(candidate.email, email)
                            && !expired(candidate)) {
                        session = candidate;
                        break iterate;
                    }
                }
            }
        }

        // Update the last accessed date if we've found the session.
        // NB this is outside of the DirectoryStream block to
        // avoid any potential clash between input and output streams
        // (When updating the Last accessed date)
        if (!expired(session)) {
            updateLastAccess(session);
            result = session;
        }

        return result;
    }

    /**
     * Determines whether a session exists for the given ID.
     *
     * @param id The ID to check.
     * @return If the ID is not blank and a corresponding session exists, true.
     * @throws IOException If a filesystem error occurs.
     */
    private boolean exists(String id) throws IOException {
        return StringUtils.isNotBlank(id) && Files.exists(sessionPath(id));
    }


    /**
     * Iterates all sessions and deletes expired ones.
     *
     * @throws IOException If a filesystem error occurs.
     */
    public void deleteExpiredSessions() throws IOException {

        List<Session> expired = new ArrayList<>();

        // Find the session we're looking for:
        iterate:
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessions)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {

                    // Examine each session to see if it has expired:
                    Session session = read(entry);
                    if (expired(session)) {
                        expired.add(session);
                    }
                }
            }
        }

        // Delete expired sessions:
        for (Session session : expired) {
            System.out.println("Deleting expired session " + session.id);
            Files.delete(sessionPath(session.id));
        }
    }

    /**
     * Determines whether the given session has expired.
     *
     * @param session The session to check.
     * @return If the session is not null and the last access time is
     * more than 60 minutes in the past, true.
     */
    private boolean expired(Session session) {
        boolean result = false;

        if (session != null) {
            Calendar expiry = Calendar.getInstance();
            expiry.add(Calendar.MINUTE, -60);
            result = session.lastAccess.before(expiry.getTime());
        }

        return result;
    }

    /**
     * Updates the last access time and saves the session to disk.
     *
     * @param session The session to update.
     * @throws IOException If a filesystem error occurs.
     */
    private void updateLastAccess(Session session) throws IOException {
        if (session != null) {
            session.lastAccess = new Date();
            write(session);
        }
    }

    /**
     * Determines the filesystem path for the given session ID.
     *
     * @param id The ID to get a path for.
     * @return The path, or null if the ID is blank.
     */
    private Path sessionPath(String id) {
        Path result = null;

        if (StringUtils.isNotBlank(id)) {
            String sessionFileName = PathUtils.toFilename(id);
            sessionFileName += ".json";
            result = sessions.resolve(sessionFileName);
        }

        return result;
    }

    /**
     * Reads the session with the given ID from disk.
     *
     * @param id The ID to be read.
     * @return The session, or
     * @throws IOException
     */
    private Session read(String id) throws IOException {
        return read(sessionPath(id));
    }

    /**
     * Reads a {@link com.github.onsdigital.zebedee.json.Session} object
     * from the given {@link java.nio.file.Path}
     *
     * @param path The path to read from.
     * @return The read session.
     * @throws IOException If a filesystem error occurs.
     */
    private Session read(Path path) throws IOException {
        Session session = null;

        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                session = Serialiser.deserialise(input, Session.class);
            }
        }

        return session;
    }

    /**
     * Writes a session object to disk.
     *
     * @param session The {@link com.github.onsdigital.zebedee.json.Session} to be written.
     * @throws IOException If a filesystem error occurs.
     */
    private void write(Session session) throws IOException {
        Path path = sessionPath(session.id);
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, session);
        }
    }

    // TODO: Add an expiry method to delete old sessions.
}
