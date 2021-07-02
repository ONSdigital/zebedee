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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * @author Scott Morse
 * SessionsAPIServiceImpl is a replacement implemtation for {@link SessionsServiceImpl}
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
     * @param session the {@link Session} to check.
     * @return true if expired, false otherwise.
     * @deprecated This is a deprecated method and not supported by this implementation
     * <p>
     * Check if the provided {@link Session} is expired.
     */
    @Override
    public boolean expired(Session session) {
        return session == null;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param none.
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get() throws IOException {
        info().log("Session get() - no-Op.");
        return null;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param token/kid - the access token to be decoded, verified and stored and key id.
     * @throws IOException for any problem verifying a token or storing a session in threadlocal.
     */
    @Override
    public void set(String token, String kid) throws IOException {
        info().log("Session set(String token, String kid) - no-Op.");
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
