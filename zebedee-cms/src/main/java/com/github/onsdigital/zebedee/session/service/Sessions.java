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
     *
     * @deprecated Using the new JWT based sessions, sessions are never created within zebedee as the JWT token
     *             issued by the dp-identity-api replaces the sessions in zebedee. Once migration to the dp-identity-api
     *             is completed this method will be removed.
     */
    @Deprecated
    Session create(User user) throws IOException;

    /**
     * Get a {@link Session} from the provided {@link HttpServletRequest}.
     *
     * @param request the {@link HttpServletRequest} to get the session for.
     * @return a {@link Session} instance if once exists returns null otherwise.
     * @throws IOException for any problem getting a sesison from the request.
     *
     * @deprecated Since the new JWT sessions implementation can only get the session of the current user, a single
     *             {@link this#get()} method is provided. Once migration to the new JWT sessions is completed all
     *             references to this method that are not simply repeating the
     *             {@link com.github.onsdigital.zebedee.filters.AuthenticationFilter} should be should be updated to
     *             use {@link this#get()} instead. If the call is duplicating the filter, then it should be removed
     *             so as not to waste compute and request latency.
     */
    @Deprecated
    Session get(HttpServletRequest request) throws IOException;

    /**
     * Get a {@link Session} by it's ID.
     *
     * @param id the ID of the session to get,
     * @return the {@link Session} instance if it exists and is not expired.
     * @throws IOException for any problems getting the session.
     *
     * @deprecated Since the new JWT sessions implementation can only get the session of the current user, a single
     *             {@link this#get()} method is provided. Once migration to the new JWT sessions is completed all
     *             references to this method should be updated to use the {@link this#get()} instead.
     */
    @Deprecated
    Session get(String id) throws IOException;

    /**
     * Find a {@link Session} assocaited with the user email.
     *
     * @param email the user email address to find.
     * @return a {@link Session} for the requested email if it exists and is not expired. Return null otherwise.
     * @throws IOException for any problems getting the session.
     *
     * @deprecated The JWT based session lookup can only look up the session of the current user. Any code still
     *             referencing this method needs to be reworked so that the current users' session is used. Once the
     *             migration to the dp-identity-api is complete this method will be removed.
     */
    @Deprecated
    Session find(String email) throws IOException;

    /**
     * Get a session object.
     *
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
     *
     * @deprecated This method is deprecated as it becomes redundant once we migrate to the JWT sessions. Once this
     *             migration has been completed the users' JWT (which is essentially the new session) is validated
     *             by the {@link com.github.onsdigital.zebedee.filters.AuthenticationFilter}. If the JWT is found to be
     *             expired by the {@link com.github.onsdigital.zebedee.filters.AuthenticationFilter} returns a 401
     *             unauthorised to the user so we would never get far enough in the execution to actually call this.
     */
    @Deprecated
    boolean expired(Session session);
}
