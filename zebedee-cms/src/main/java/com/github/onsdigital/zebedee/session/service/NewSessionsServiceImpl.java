package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.session.service.client.SessionClient;
import com.session.service.client.SessionClientImpl;
import com.session.service.entities.SessionCreated;
import com.session.service.error.SessionClientException;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

public class NewSessionsServiceImpl implements Sessions {

    private SessionClient client;

    public NewSessionsServiceImpl(SessionClient sessionClient) {
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
        SessionCreated sessionCreated = client.createNewSession(user.getEmail());
        com.session.service.Session clientSession = client.getSessionByID(sessionCreated.getId());

        if (clientSession == null) {
            throw new IOException("client has failed to retrieve the session");
        }

        Session session = createZebedeeSession(clientSession);
        return session;
    }

    /**
     * Get a {@link Session} from the provided {@link HttpServletRequest}.
     *
     * @param request the {@link HttpServletRequest} to get the session for.
     * @return a {@link Session} instance if once exists returns null otherwise.
     * @throws IOException for any problem getting a sesison from the request.
     */
    @Override
    public Session get(HttpServletRequest request) throws IOException {
        String token = RequestUtils.getSessionId(request);
        return get(token);
    }

    /**
     * Get a {@link Session} by it's ID.
     *
     * @param id the ID of the session to get,
     * @return the {@link Session} instance if it exists and is not expired.
     * @throws IOException for any problems getting the session.
     */
    @Override
    public Session get(String id) throws IOException {
        com.session.service.Session clientSession = client.getSessionByID(id);
        
        return createZebedeeSession(clientSession);
    }

    /**
     * Find a {@link Session} assocaited with the user email.
     *
     * @param email the user email address to find.
     * @return a {@link Session} for the requested email if it exists and is not expired. Return null otherwise.
     * @throws IOException for any problems getting the session.
     */
    @Override
    public Session find(String email) throws IOException {
        com.session.service.Session clientSession = client.getSessionByEmail(email);

        Session session = createZebedeeSession(clientSession);
        return session;
    }

    /**
     * Check if an active {@link Session} exists with the provided ID.
     *
     * @param id the session ID to find.
     * @return true if an active session exists with this ID, false otherwise.
     * @throws IOException problem checking the session exists.
     */
    @Override
    public boolean exists(String id) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Check if the provided {@link Session} is expired.
     *
     * @param session the {@link Session} to check.
     * @return true if expired, false otherwise.
     */
    @Override
    public boolean expired(Session session) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the expiry date of the provided {@link Session}
     *
     * @param session the  {@link Session} to use.
     * @return the sessions expiration date time as a {@link Date} instance.
     */
    @Override
    public Date getExpiryDate(Session session) {
        throw new UnsupportedOperationException();
    }

    private Session createZebedeeSession(com.session.service.Session clientSession) {
        Session session = new Session();

        if (clientSession.getId() == null || StringUtils.isEmpty(clientSession.getId())) {
            throw new SessionClientException("client has returned a session with a null/empty id");
        }

        if (clientSession.getEmail() == null || StringUtils.isEmpty(clientSession.getEmail())) {
            throw new SessionClientException("client has returned a session with a null/empty email");
        }

        session.setId(clientSession.getId());
        session.setEmail(clientSession.getEmail());
        session.setLastAccess(clientSession.getLastAccess());
        session.setStart(clientSession.getStart());
        
        return session;
    }
}
