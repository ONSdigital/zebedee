package com.github.onsdigital.zebedee.session.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.serialiser.JSONSerialiser;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by dave on 23/05/2017.
 */
public class SessionsStoreImpl implements SessionsStore {

    private static final String DS_STORE_FILE = ".DS_Store";
    private static final String JSON_EXT = ".json";

    private Path sessionsPath;

    private static final Predicate<Path> isSessionFile = (p) -> p != null && !Files.isDirectory(p)
            && p.getFileName().toString().endsWith(JSON_EXT);

    private JSONSerialiser<Session> sessionJSONSerialiser;

    public SessionsStoreImpl(Path sessionsPath) {
        this.sessionsPath = sessionsPath;
        this.sessionJSONSerialiser = new JSONSerialiser(Session.class);
    }

    /**
     * Determines the filesystem path for the given session ID.
     *
     * @param id The ID to get a path for.
     * @return The path, or null if the ID is blank.
     */
    private Path getPath(Session s) {
        return getPath(s.getId());
    }

    private Path getPath(String id) {
        Path result = null;
        if (StringUtils.isNotBlank(id)) {
            String sessionFileName = PathUtils.toFilename(id);
            sessionFileName += JSON_EXT;
            result = sessionsPath.resolve(sessionFileName);
        }
        return result;
    }

    /**
     * Writes a session object to disk.
     *
     * @param session The {@link Session} to be written.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public synchronized void write(Session session) throws IOException {
        try (OutputStream output = Files.newOutputStream(getPath(session))) {
            Serialiser.serialise(output, session);
        }
    }

    /**
     * Reads a {@link Session} object
     * from the given {@link java.nio.file.Path}
     *
     * @param path The path to read from.
     * @return The read session.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public synchronized Session read(Path path) throws IOException {
        Session session = null;

        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                session = sessionJSONSerialiser.deserialiseQuietly(input, path);
            }
        }
        return session;
    }

    @Override
    public boolean exists(String id) throws IOException {
        return StringUtils.isNotBlank(id) && Files.exists(getPath(id));
    }

    @Override
    public Session find(String email) throws IOException {
        Session candidate = null;
        Session result = null;

        iterate:
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionsPath)) {
            for (Path entry : stream) {
                if (isSessionFile.test(entry)) {
                    candidate = read(entry);
                    if (candidate != null && StringUtils.equalsIgnoreCase(candidate.getEmail(),
                            PathUtils.standardise(email))) {
                        return candidate;
                    }
                    candidate = null;
                }
            }
        }
        return result;
    }

    @Override
    public List<Session> filterSessions(Predicate<Session> criteria) throws IOException {
        List<Session> results = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionsPath)) {
            for (Path entry : stream) {
                if (isSessionFile.test(entry)) {
                    Session s = read(entry);
                    if (criteria.test(s)) {
                        results.add(s);
                    }
                }
            }
        }
        return results;
    }

    @Override
    public void delete(Path p) throws IOException {
        Files.delete(p);
    }
}
