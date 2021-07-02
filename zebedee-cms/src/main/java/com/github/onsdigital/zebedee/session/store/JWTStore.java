package com.github.onsdigital.zebedee.session.store;

import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;

/**
 * JWTStore:  class, when instantiated, will
 * *********  allow for access token verification,
 *            storage (set()) and retrieval (get())
 *            from threadlocal.
 *           
 *            Implements Sessions interface.
 */
public class JWTStore implements Sessions {

    private Map<String, String> rsaKeyMap;

    // class constructor - takes HashMap<String, String> as param.
    public JWTStore(Map<String, String> rsaKeyMap) {
        this.rsaKeyMap = rsaKeyMap;
    }

    /**
     * Find a {@link Session} associated with the user email - defaults to the NoOp impl.
     */
    @Override
    public Session find(String email) throws IOException {
        return null;
    }

    /**
     * Create a new {@link Session} for the user - defaults to the NoOp impl.
     */
    @Override
    public Session create(User user) throws IOException {
        return null;
    }

    /**
     * Check if the provided {@link Session} is expired - defaults to the NoOp impl.
     */
    @Override
    public boolean expired(Session session) {
        return session == null;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param id the {@link String} to get the session object from thread local for.
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get(String id) throws IOException {
        return get();
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param id the {@link HttpServletRequest} to get the session object from thread local for.
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get(HttpServletRequest id) throws IOException {
        return get();
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
    }

}
