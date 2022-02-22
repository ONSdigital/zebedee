package com.github.onsdigital.zebedee.session.service;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.session.model.LegacySession;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.store.LegacySessionsStore;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;

/**
 * Created by david on 12/03/2015.
 *
 * @deprecated In favour of the transition ThreadLocalSessionsServiceImpl. The ThreadLocalSessionsServiceImpl should be
 *             used in all cases. This class will be removed.
 */
@Deprecated
public class SessionsServiceImpl extends TimerTask implements Sessions {

    static final int EXPIRY_UNIT = Calendar.MINUTE;
    static final int EXPIRY_AMOUNT = 60;
    private static final String DELETING_SESSION_MSG = "Deleting expired session";

    private Supplier<String> randomIdGenerator = Random::id;
    private ClosableTimer timer;
    protected LegacySessionsStore legacySessionsStore;

    public SessionsServiceImpl(LegacySessionsStore legacySessionsStore) {
        this.legacySessionsStore = legacySessionsStore;

        // Run every minute after the first minute:
        info().log("starting sessions timer");
        timer = new ClosableTimer("Florence sessions timer", true);
        timer.schedule(this, 60 * 1000, 60 * 1000);
        Runtime.getRuntime().addShutdownHook(new Thread(ClosableTimer::close));

    }

    /**
     * This is the entrypoint for {@link java.util.TimerTask}.
     */
    @Override
    public void run() {
        try {
            deleteExpiredSessions();
        } catch (IOException e) {
            error().exception(e).log("deleting expired sessions failed due to unexpected error");
        }
    }

    /**
     * Creates a new session.
     *
     * @param email the email of the user the session is being created for.
     * @return A session for the email. If one exists it returns that else it
     * creates a new one. If the supplied email is blank then null will
     * be returned.
     * @throws java.io.IOException If a filesystem error occurs.
     */
    @Override
    public Session create(String email) throws IOException {
        Session session = null;

        if (StringUtils.isNotBlank(email)) {

            // Check for an existing session:
            LegacySession legacySession = find(email);

            // Otherwise go ahead and create
            if (legacySession == null || expired(legacySession)) {
                legacySession = new LegacySession(randomIdGenerator.get(),email);
                legacySessionsStore.write(legacySession);
            }

            session = new Session(legacySession.getId(), legacySession.getEmail());
        }

        return session;
    }

    /**
     * Gets the record for an existing session.
     *
     * @param id The session ID in order to locate the session record.
     * @return The requested session, unless the ID is blank or no record exists
     * for this ID.
     * @throws java.io.IOException If a filesystem error occurs.
     */
    protected Session get(String id) throws IOException {
        Session session = null;

        // Check the session record exists:
        if (legacySessionsStore.exists(id)) {
            // Deserialise the json:
            LegacySession legacySession = legacySessionsStore.read(id);
            if (!expired(legacySession)) {
                updateLastAccess(legacySession);
                session = new Session(legacySession.getId(), legacySession.getEmail());
            }
            else{
               warn().log("session found expired, this is a known error during session get");
            }
        }
        return session;
    }

    /**
     * Locates a session from the user's email address (user ID).
     *
     * @param email The email.
     * @return An existing session, if found.
     * @throws IOException If a filesystem error occurs.
     */
    private LegacySession find(String email) throws IOException {
        LegacySession session = legacySessionsStore.find(email);
        if (!expired(session)) {
            updateLastAccess(session);
        }
        else{
            warn().log("session found expired, this is a known error during session find");
        }
        return session;
    }

    /**
     * Iterates all sessions and deletes expired ones.
     *
     * @throws IOException If a filesystem error occurs.
     */
    void deleteExpiredSessions() throws IOException {
        Predicate<LegacySession> isExpired = this::expired;
        List<LegacySession> expired = legacySessionsStore.filterSessions(isExpired);

        for (LegacySession s : expired) {
            info().data("user", s.getEmail()).log(DELETING_SESSION_MSG);
            legacySessionsStore.delete(s.getId());
        }
    }

    /**
     * Determines whether the given session has expired.
     *
     * @param session The session to check.
     * @return If the session is not null and the last access time is
     * more than 60 minutes in the past, true.
     */
    boolean expired(LegacySession session) {
        boolean result = false;

        if (session != null) {
            Calendar expiry = Calendar.getInstance();
            expiry.add(EXPIRY_UNIT, -EXPIRY_AMOUNT);
            result = session.getLastAccess().before(expiry.getTime());
        }

        return result;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get() {
        info().log("Session get() - no-Op.");
        return null;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param token - the access token to be decoded, verified and stored.
     * @throws IOException for any problem verifying a token or storing a session in threadlocal.
     */
    @Override
    public void set(String token) throws IOException {
        info().log("Session set(String token) - no-Op.");
    }

    /**
     * Reset the thread by removing the current {@link ThreadLocal} value. If threads are being recycled to serve new
     * requests then this method must be called on each new request to ensure that sessions do not leak from one request
     * to the next causing potential for privilege excalation.
     */
    @Override
    public void resetThread() {
        info().log("Session resetThread() - no-Op.");
    }

    /**
     * Updates the last access time and saves the session to disk.
     *
     * @param session The session to update.
     * @throws IOException If a filesystem error occurs.
     */
    private void updateLastAccess(LegacySession session) throws IOException {
        if (session != null) {
            session.setLastAccess(new Date());
            legacySessionsStore.write(session);
        }
    }

    static class ClosableTimer extends Timer{

        public ClosableTimer(String name, boolean val){
            super(name, val);
        }

        public static void close() {
           info().log("session timer has shut down, this needs to be investigated if it happened unexpectedly");
        }
    }
}


