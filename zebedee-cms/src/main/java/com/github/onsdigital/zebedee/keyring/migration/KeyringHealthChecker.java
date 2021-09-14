package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.session.model.Session;

/**
 * Defines a collection key health checker.
 */
public interface KeyringHealthChecker {

    /**
     * Verify the user's collection keying contains the expected collection keys.
     *
     * @param session the {@link Session} of the user being validated.
     */
    void check(Session session);
}
