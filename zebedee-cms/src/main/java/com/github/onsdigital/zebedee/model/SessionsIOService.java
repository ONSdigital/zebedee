package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.Session;
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
public class SessionsIOService {

    private static final String DS_STORE_FILE = ".DS_Store";
    private static final String JSON_EXT = ".json";
    private Path sessionsPath;

    public SessionsIOService(Path sessionsPath) {
        this.sessionsPath = sessionsPath;
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
     * @param session The {@link com.github.onsdigital.zebedee.json.Session} to be written.
     * @throws IOException If a filesystem error occurs.
     */
    synchronized void write(Session session) throws IOException {
        try (OutputStream output = Files.newOutputStream(getPath(session))) {
            Serialiser.serialise(output, session);
        }
    }

    /**
     * Reads a {@link com.github.onsdigital.zebedee.json.Session} object
     * from the given {@link java.nio.file.Path}
     *
     * @param path The path to read from.
     * @return The read session.
     * @throws IOException If a filesystem error occurs.
     */
    synchronized Session read(Path path) throws IOException {
        Session session = null;

        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                session = Serialiser.deserialise(input, Session.class);
            }
        }
        return session;
    }

    public boolean exists(String id) throws IOException {
        return StringUtils.isNotBlank(id) && Files.exists(getPath(id));
    }

    public Session find(String email) throws IOException {
        Session candidate = null;

        iterate:
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionsPath)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry) && !entry.endsWith(DS_STORE_FILE)) {
                    candidate = read(entry);
                    if (StringUtils.equalsIgnoreCase(candidate.getEmail(), PathUtils.standardise(email))) {
                        break iterate;
                    }
                }
            }
        }
        return candidate;
    }

    public List<Session> filterSessions(Predicate<Session> criteria) throws IOException {
        List<Session> results = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionsPath)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    Session s = read(entry);
                    if (criteria.test(s)) {
                        results.add(s);
                    }
                }
            }
        }
        return results;
    }

    public void delete(Path p) throws IOException {
        Files.delete(p);
    }
}
