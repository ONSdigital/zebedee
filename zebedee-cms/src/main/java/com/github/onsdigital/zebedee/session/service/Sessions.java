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
     *
     * TODO: Remove this method once the migration to JWT based sessions is complete
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
     *
     * TODO: remove all usage of this method after migration to JWT based sessions is complete
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
     *
     * TODO: Replace all calls to this method with `Sessions.get()` instead after migration to the JWT based sessions
     *       is complete
     */
    @Deprecated
    Session get(String id) throws IOException;

    /**
     * Get a session object.
     *
     * @return the {@link Session} instance if it exists and is not expired.
     * @throws IOException for any problems getting the session.
     */
    Session get() throws IOException;

    /**
     * Set user's data in a ThreadLocal object.
     *
     * @param token the access token,
     *        
     * @return nothing.
     * @throws IOException for any problems getting the session.
     */
    void set(String token) throws IOException;
}
