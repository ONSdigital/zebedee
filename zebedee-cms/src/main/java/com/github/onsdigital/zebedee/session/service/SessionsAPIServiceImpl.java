package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.session.service.client.SessionClient;
import com.github.onsdigital.session.service.entities.SessionCreated;
import com.github.onsdigital.session.service.error.SessionClientException;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Scott Morse
 * NewSessionsServiceImpl is a replacement implemtation for {@link SessionsServiceImpl}
 * It makes use of dp-sessions-api using dp-session-service-client-java
 */
public class SessionsAPIServiceImpl implements Sessions {

    private SessionClient client;

    /**
     * Constructor requires a configured {@link SessionClient}
     *
     * @param sessionClient
     */
    public SessionsAPIServiceImpl(SessionClient sessionClient) {
        this.client = sessionClient;
    }

    /**
     * Create a new {@link Session} for the user.
     *
     * @param user the {@link User} the session is being created for.
     * @return a {@link Session} instance for the user.
     * @throws IOException problem creating a session.
     */
    @Override
    public Session create(User user) throws IOException {
        if (user == null) {
            throw new IOException("create session requires user but was null");
        }

        if (StringUtils.isEmpty(user.getEmail())) {
            throw new IOException("create session requires user email but was null or empty");
        }

        SessionCreated sessionCreated = client.createNewSession(user.getEmail());
        if (sessionCreated == null) {
            throw new IOException("unexpected error creating new session expected session but was null");
        }

        com.github.onsdigital.session.service.Session apiSession = client.getSessionByID(sessionCreated.getId());
        if (apiSession == null) {
            throw new IOException("client failed to retrieve session from sessions api");
        }

        Session session = createZebedeeSession(apiSession);
        return session;
    }

    /**
     * Get a {@link Session} from the provided {@link HttpServletRequest}.
     *
     * @param id the {@link HttpServletRequest} to get the session for.
     * @return a {@link Session} instance if once exists returns null otherwise.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get(HttpServletRequest id) throws IOException {
        String token = RequestUtils.getSessionId(id);
        return get(token);
    }

    /**
     * Get a {@link Session} by it's ID.
     *
     * @param id the ID of the session to get,
     * @return the {@link Session} instance if it exists and is not expired. If the id is null or empty will return null.
     * @throws IOException for any problems getting the session.
     */
    @Override
    public Session get(String id) throws IOException {
        if (StringUtils.isEmpty(id)) {
            return null;
        }

        com.github.onsdigital.session.service.Session cachedSession = client.getSessionByID(id);
        if (cachedSession == null) {
            return null;
        }

        return createZebedeeSession(cachedSession);
    }

    /**
     * Find a {@link Session} associated with the user email.
     *
     * @param email the user email address to find.
     * @return a {@link Session} for the requested email if it exists and is not expired. Return null otherwise.
     * @throws IOException for any problems getting the session.
     */
    @Override
    public Session find(String email) throws IOException, SessionClientException {
        if (StringUtils.isEmpty(email)) {
            return null;
        }

        com.github.onsdigital.session.service.Session cachedSession = client.getSessionByEmail(email);

        if (cachedSession == null) {
            return null;
        }
        return createZebedeeSession(cachedSession);
    }

    /**
     * @param id the session ID to find.
     * @return true if an active session exists with this ID, false otherwise.
     * @throws IOException problem checking the session exists.
     * @deprecated This is a deprecated method and not supported by this implementation
     * <p>
     * Check if an active {@link Session} exists with the provided ID.
     */
    @Override
    public boolean exists(String id) throws IOException {
        throw new UnsupportedOperationException("exists is a deprecated method and not supported by this sessions implementation.");
    }

    /**
     * @param session the {@link Session} to check.
     * @return true if expired, false otherwise.
     * @deprecated This is a deprecated method and not supported by this implementation
     * <p>
     * Check if the provided {@link Session} is expired.
     */
    @Override
    public boolean expired(Session session) {
        throw new UnsupportedOperationException("expired is a deprecated method and not supported by this sessions implementation.");
    }

    /**
     * @param session the  {@link Session} to use.
     * @return the sessions expiration date time as a {@link Date} instance.
     * @deprecated This is a deprecated method and not supported by this implementation
     * <p>
     * Get the expiry date of the provided {@link Session}
     */
    @Override
    public Date getExpiryDate(Session session) {
        throw new UnsupportedOperationException("getExpiryDate is a deprecated method and not supported by this sessions implementation.");
    }

    private Session createZebedeeSession(com.github.onsdigital.session.service.Session sess) throws IOException {
        if (sess == null) {
            throw new IOException("expected cached session but returned null");
        }

        if (isEmpty(sess.getId())) {
            throw new IOException("client has returned a session with a null/empty id");
        }

        if (isEmpty(sess.getEmail())) {
            throw new IOException("client has returned a session with a null/empty email");
        }

        return Session.fromAPIModel(sess);
    }
}
