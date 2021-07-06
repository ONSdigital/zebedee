package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface Sessions {

    /**
     * Create a new {@link Session} for the user.
     *
     * @param user the {@link User} the session is being created for.
     * @return a {@link Session} instance for the user.
     * @throws IOException problem creating a session.
     */
    Session create(User user) throws IOException;

    /**
     * Get a {@link Session} from the provided {@link HttpServletRequest}.
     *
     * @param request the {@link HttpServletRequest} to get the session for.
     * @return a {@link Session} instance if once exists returns null otherwise.
     * @throws IOException for any problem getting a sesison from the request.
     */
    Session get(HttpServletRequest request) throws IOException;

    /**
     * Get a {@link Session} by it's ID.
     *
     * @param id the ID of the session to get,
     * @return the {@link Session} instance if it exists and is not expired.
     * @throws IOException for any problems getting the session.
     */
    Session get(String id) throws IOException;

    /**
     * Find a {@link Session} assocaited with the user email.
     *
     * @param email the user email address to find.
     * @return a {@link Session} for the requested email if it exists and is not expired. Return null otherwise.
     * @throws IOException for any problems getting the session.
     */
    Session find(String email) throws IOException;

    /**
     * Get a session object.
     *
     * @param none,
     * @return the {@link Session} instance if it exists and is not expired.
     * @throws IOException for any problems getting the session.
     */
    Session get() throws IOException;

    /**
     * Set user's data in a threadlocal object.
     *
     * @param token the access token,
     *        
     * @return nothing.
     * @throws IOException for any problems getting the session.
     */
    void set(String token) throws IOException;

    /**
     * Check if the provided {@link Session} is expired.
     *
     * @param session the {@link Session} to check.
     * @return true if expired, false otherwise.
     */
    @Deprecated
    boolean expired(Session session);
}
