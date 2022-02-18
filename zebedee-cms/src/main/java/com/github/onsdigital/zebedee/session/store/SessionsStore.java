package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.zebedee.session.model.LegacySession;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by dave on 25/05/2017.
 *
 * @deprecated as zebedee will no longer need a persistent store for sessions after migration to JWT sessions and the
 *             dp-identity-api. Once the migration is complete this will be removed.
 */
@Deprecated
public interface SessionsStore {

    /**
     * Persist a {@link LegacySession}.
     *
     * @param session the {@link LegacySession} to persist.
     * @throws IOException
     */
    void write(LegacySession session) throws IOException;

    /**
     * Read a {@link LegacySession} specified by the path parameter.
     *
     * @param id the session id of the {@link LegacySession} to read.
     * @return the requested {@link LegacySession} if it exists.
     * @throws IOException
     */
    LegacySession read(String id) throws IOException;

    /**
     * Determined if a {@link LegacySession} exists with the specified ID.
     *
     * @param id the {@link LegacySession#id} to search for.
     * @return true if a session exists, false otherwise.
     * @throws IOException unexpected error while looking for session.
     */
    boolean exists(String id) throws IOException;

    /**
     * Get a {@link LegacySession} by the {@link com.github.onsdigital.zebedee.json.User#email}
     *
     * @param email the email of the user to the session belongs to.
     * @return the {@link LegacySession} if it exists, null otherwise.
     * @throws IOException unexpected error while looking for session.
     */
    LegacySession find(String email) throws IOException;

    /**
     * Return a {@link List} of {@link LegacySession} matching the specified criteria.
     *
     * @param criteria {@link Predicate} defining the criteria to filter on/
     * @return {@link List} of {@link LegacySession} matching the criteria.
     * @throws IOException unexpected error while looking for session.
     */
    List<LegacySession> filterSessions(Predicate<LegacySession> criteria) throws IOException;

    /**
     * Delete {@link LegacySession} if it exists.
     *
     * @param id the id of the session to delete.
     * @throws IOException unexpected error while attempting to delete the session.
     */
    void delete(String id) throws IOException;
}
