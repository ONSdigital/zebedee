package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.zebedee.session.model.Session;

import java.io.IOException;

public interface Sessions {

    /**
     * Create a new {@link Session} for the user.
     *
     * @param email the email of the user the session is being created for.
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
    Session create(String email) throws IOException;

    /**
     * Get a session object.
     *
     * @return the {@link Session} instance if it exists and is not expired.
     */
    Session get();

    /**
     * Set user's data in a ThreadLocal object.
     *
     * @param token the access token,
     *        
     * @return nothing.
     * @throws IOException for any problems getting the session.
     */
    void set(String token) throws IOException;

    /**
     * Reset the thread by removing the current {@link ThreadLocal} value. If threads are being recycled to serve new
     * requests then this method must be called on each new request to ensure that sessions do not leak from one request
     * to the next causing potential for privilege excalation.
     */
    void resetThread();
}
