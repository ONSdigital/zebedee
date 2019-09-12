package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;

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
     * Check if an active {@link Session} exists with the provided ID.
     *
     * @param id the session ID to find.
     * @return true if an active session exists with this ID, false otherwise.
     * @throws IOException problem checking the session exists.
     */
    boolean exists(String id) throws IOException;

    /**
     * Check if the provided {@link Session} is expired.
     *
     * @param session the {@link Session} to check.
     * @return true if expired, false otherwise.
     */
    boolean expired(Session session);

    /**
     * Get the expiry date of the provided {@link Session}
     *
     * @param session the  {@link Session} to use.
     * @return the sessions expiration date time as a {@link Date} instance.
     */
    Date getExpiryDate(Session session);
}
