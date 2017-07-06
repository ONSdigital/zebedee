package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by dave on 25/05/2017.
 */
public interface SessionsStore {

    /**
     * Persist a {@link Session}.
     *
     * @param session the {@link Session} to persist.
     * @throws IOException
     */
    void write(Session session) throws IOException;

    /**
     * Read a {@link Session} specified by the path parameter.
     *
     * @param path the {@link Path} of the {@link Session} to read.
     * @return the requested {@link Session} if it exists.
     * @throws IOException
     */
    Session read(Path path) throws IOException;

    /**
     * Determined if a {@link Session} exists with the specified ID.
     *
     * @param id the {@link Session#id} to search for.
     * @return true if a session exists, false otherwise.
     * @throws IOException unexpected error while looking for session.
     */
    boolean exists(String id) throws IOException;

    /**
     * Get a {@link Session} by the {@link com.github.onsdigital.zebedee.json.User#email}
     *
     * @param email the email of the user to the session belongs to.
     * @return the {@link Session} if it exists, null otherwise.
     * @throws IOException unexpected error while looking for session.
     */
    Session find(String email) throws IOException;

    /**
     * Return a {@link List} of {@link Session} matching the specified criteria.
     *
     * @param criteria {@link Predicate} defining the criteria to filter on/
     * @return {@link List} of {@link Session} matching the criteria.
     * @throws IOException unexpected error while looking for session.
     */
    List<Session> filterSessions(Predicate<Session> criteria) throws IOException;

    /**
     * Delete {@link Session} if it exists.
     *
     * @param p the {@link Path} of the session to delete.
     * @throws IOException unexpected error while attempting to delete the session.
     */
    void delete(Path p) throws IOException;
}
