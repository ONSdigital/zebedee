package com.github.onsdigital.zebedee.session.service;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.session.store.SessionsStoreImpl;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Created by david on 12/03/2015.
 */
public class SessionsService extends TimerTask {

    private static final String DELETING_SESSION_MSG = "Deleting expired session";
    private static final String SESSION_ID_PARAM = "sessionId";

    private Supplier<String> randomIdGenerator = () -> Random.id();
    private SessionsStoreImpl sessionsStore;

    int expiryUnit = Calendar.MINUTE;
    int expiryAmount = 60;
    Timer timer;
    private Path sessionsPath;

    public SessionsService(Path sessionsPath) {
        this.sessionsPath = sessionsPath;
        this.sessionsStore = new SessionsStoreImpl(sessionsPath);

        // Run every minute after the first minute:
        timer = new Timer("Florence sessions timer", true);
        timer.schedule(this, 60 * 1000, 60 * 1000);
    }

    public void setExpiry(int expiryAmount, int expiryUnit) {
        this.expiryAmount = expiryAmount;
        this.expiryUnit = expiryUnit;
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
     * @param user the user to create a session for
     * @return A session for the email. If one exists it returns that else it
     * creates a new one. If the supplied email is blank then null will
     * be returned.
     * @throws java.io.IOException If a filesystem error occurs.
     */
    public Session create(User user) throws IOException {
        Session session = null;

        if (StringUtils.isNotBlank(user.getEmail())) {

            // Check for an existing session:
            session = find(user.getEmail());

            // Otherwise go ahead and create
            if (session == null) {
                session = new Session();
                session.setId(randomIdGenerator.get());
                session.setEmail(user.getEmail());
                sessionsStore.write(session);
            }
        }

        return session;
    }

    /**
     * Gets the record for an existing session.
     *
     * @param request The {@link HttpServletRequest}.
     * @return The requested session, unless the ID is blank or no record exists
     * for this ID.
     * @throws java.io.IOException If a filesystem error occurs.
     */
    public Session get(HttpServletRequest request) throws IOException {
        String token = RequestUtils.getSessionId(request);
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
        if (sessionsStore.exists(id)) {
            // Deserialise the json:
            Session session = sessionsStore.read(sessionPath(id));
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
        Session session = sessionsStore.find(email);
        if (!expired(session)) {
            updateLastAccess(session);
        }
        return session;
    }

    /**
     * Determines whether a session exists for the given ID.
     *
     * @param id The ID to check.
     * @return If the ID is not blank and a corresponding session exists, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean exists(String id) throws IOException {
        return sessionsStore.exists(id);
    }


    /**
     * Iterates all sessions and deletes expired ones.
     *
     * @throws IOException If a filesystem error occurs.
     */
    public void deleteExpiredSessions() throws IOException {
        Predicate<Session> isExpired = (session) -> expired(session);
        List<Session> expired = sessionsStore.filterSessions(isExpired);

        for (Session s : expired) {
            logDebug(DELETING_SESSION_MSG)
                    .addParameter(SESSION_ID_PARAM, s.getId())
                    .log();
            sessionsStore.delete(sessionPath(s.getId()));
        }
    }

    /**
     * Determines whether the given session has expired.
     *
     * @param session The session to check.
     * @return If the session is not null and the last access time is
     * more than 60 minutes in the past, true.
     */
    public boolean expired(Session session) {
        boolean result = false;

        if (session != null) {
            Calendar expiry = Calendar.getInstance();
            expiry.add(expiryUnit, -expiryAmount);
            result = session.getLastAccess().before(expiry.getTime());
        }

        return result;
    }


    /**
     * Updates the last access time and saves the session to disk.
     *
     * @param session The session to update.
     * @throws IOException If a filesystem error occurs.
     */
    public void updateLastAccess(Session session) throws IOException {
        if (session != null) {
            session.setLastAccess(new Date());
            sessionsStore.write(session);
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
            result = sessionsPath.resolve(sessionFileName);
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
    public Session read(String id) throws IOException {
        return sessionsStore.read(sessionPath(id));
    }

    public Date getExpiryDate(Session session) {
        Date expiry = null;

        if (session != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(session.getLastAccess());
            calendar.add(expiryUnit, expiryAmount);
            expiry = calendar.getTime();
        }
        return expiry;
    }
}
