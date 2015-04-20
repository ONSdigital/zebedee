package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.AccessMapping;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Handles permissions mapping between users and {@link com.github.onsdigital.zebedee.Zebedee} functions.
 * Created by david on 12/03/2015.
 */
public class Permissions {
    private Path permissions;
    private Zebedee zebedee;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public Permissions(Path permissions, Zebedee zebedee) {
        this.permissions = permissions;
        this.zebedee = zebedee;
    }

    /**
     * Determines whether the specified user has administator permissions.
     *
     * @param email The user's emal.
     * @return True if the user is an administrator.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean isAdministrator(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return accessMapping.administrators != null && accessMapping.administrators.contains(email);
    }

    /**
     * Determines whether an administator exists.
     *
     * @return True if at least one administrator exists.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean hasAdministrator() throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return accessMapping.administrators != null && (accessMapping.administrators.size() > 0);
    }

    /**
     * Determines whether the specified user has editing rights.
     *
     * @param email The user's email.
     * @return True if the user is a member of the Digital Publishing team.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean canEdit(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return canEdit(email, accessMapping);
    }

    /**
     * Determines whether the specified user has viewing rights.
     *
     * @param email The user's email.
     * @param path  The path to be viewed.
     * @return True if the user is a member of the Digital Publishing team or
     * the user is a content owner with access to the given path or any parent path.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean canView(String email, String path) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return canEdit(email, accessMapping) || canView(email, path, accessMapping);
    }

    /**
     * Adds the specified user to the administrators, giving them administrator permissions (but not content permissions).
     *
     * <p>If no administrator exists the first call will succeed otherwise </p>
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void addAdministrator(String email, Session session) throws IOException {

        if (session == null || !zebedee.permissions.isAdministrator(session.email)) {
            return;
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.administrators == null) {
            accessMapping.administrators = new HashSet<>();
        }
        accessMapping.administrators.add(email);
        writeAccessMapping(accessMapping);
    }
    /**
     * Removes the specified user from the administrators, revoking administrative permissions (but not content permissions).
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeAdministrator(String email, Session session) throws IOException {
        if (session == null || !zebedee.permissions.isAdministrator(session.email)) {
            return;
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.administrators == null) {
            accessMapping.administrators = new HashSet<>();
        }
        accessMapping.administrators.remove(email);
        writeAccessMapping(accessMapping);
    }

    /**
     * Adds the specified user to the Digital Publishing team, giving them access to read and write all content.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void addEditor(String email, Session session) throws IOException {
        if (session == null || !zebedee.permissions.isAdministrator(session.email)) {
            return;
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.digitalPublishingTeam == null) {
            accessMapping.digitalPublishingTeam = new HashSet<>();
        }
        accessMapping.digitalPublishingTeam.add(email);
        writeAccessMapping(accessMapping);
    }


    /**
     * Removes the specified user to the Digital Publishing team, revoking access to read and write all content.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeEditor(String email, Session session) throws IOException {
        if (session == null || !zebedee.permissions.isAdministrator(session.email)) {
            return;
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.digitalPublishingTeam == null) {
            accessMapping.digitalPublishingTeam = new HashSet<>();
        }
        accessMapping.digitalPublishingTeam.remove(email);
        writeAccessMapping(accessMapping);
    }

    /**
     * Adds the specified user to the content administrators, giving them access to read content at the given path and all sub-paths.
     *
     * @param email The user's email.
     * @param path  The path under which the user will get read access.
     * @throws IOException If a filesystem error occurs.
     */
    public void addViewer(String email, String path, Session session) throws IOException {
        if (session == null || !zebedee.permissions.isAdministrator(session.email)) {
            return;
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.paths == null) {
            accessMapping.paths = new HashMap<>();
        }
        Set<String> viewers = accessMapping.paths.get(path);
        if (viewers == null) {
            viewers = new HashSet<>();
        }
        viewers.add(email);
        accessMapping.paths.put(path, viewers);
        writeAccessMapping(accessMapping);
    }

    /**
     * Adds the specified user to the content administrators, giving them access to read content at the given paths and all sub-paths.
     *
     * @param email The user's email.
     * @param paths The paths under which the user will get read access.
     * @throws IOException If a filesystem error occurs.
     */
    public void addViewer(String email, Set<String> paths, Session session) throws IOException {
        if (session == null || !zebedee.permissions.isAdministrator(session.email)) {
            return;
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.paths == null) {
            accessMapping.paths = new HashMap<>();
        }

        for (String path : paths) {
            Set<String> viewers = accessMapping.paths.get(path);
            if (viewers == null) {
                viewers = new HashSet<>();
            }
            viewers.add(email);
            accessMapping.paths.put(path, viewers);
        }

        writeAccessMapping(accessMapping);
    }

    /**
     * removes the specified user from the content owners, revoking access to read content at the given path and all sub-paths.
     *
     * @param email The user's email.
     * @param path  The path under which the user will no longer have read access.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeViewer(String email, String path, Session session) throws IOException {
        if (session == null || !zebedee.permissions.isAdministrator(session.email)) {
            return;
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.paths == null) {
            accessMapping.paths = new HashMap<>();
        }

        // Check to see if the requested path matches (or is a sub-path of) any mapping:
        for (Map.Entry<String, Set<String>> mapping : accessMapping.paths.entrySet()) {
            boolean isSubPath = StringUtils.startsWithIgnoreCase(mapping.getKey(), path);
            boolean emailMatches = mapping.getValue().contains(email);
            if (isSubPath && emailMatches) {
                mapping.getValue().remove(email);
            }
        }
        writeAccessMapping(accessMapping);
    }

    /**
     * removes the specified user from the content owners, revoking access to read content from any path.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeViewer(String email, Session session) throws IOException {
        if (session == null || !zebedee.permissions.isAdministrator(session.email)) {
            return;
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.paths == null) {
            accessMapping.paths = new HashMap<>();
        }

        // Ensure the email address is not present for any path:
        for (Map.Entry<String, Set<String>> mapping : accessMapping.paths.entrySet()) {
            mapping.getValue().remove(email);
        }
        writeAccessMapping(accessMapping);
    }

    private boolean canEdit(String email, AccessMapping accessMapping) throws IOException {
        Set<String> digitalPublishingTeam = accessMapping.digitalPublishingTeam;
        return digitalPublishingTeam!=null && digitalPublishingTeam.contains(email);
    }

    private boolean canView(String email, String path, AccessMapping accessMapping) {

        // Check to see if the requested path matches (or is a sub-path of) any mapping:
        for (Map.Entry<String, Set<String>> mapping : accessMapping.paths.entrySet()) {
            boolean isSubPath = StringUtils.startsWithIgnoreCase(path, mapping.getKey());
            boolean emailMatches = mapping.getValue().contains(email);
            if (isSubPath && emailMatches) {
                return true;
            }
        }

        // No match found:
        return false;
    }

    private AccessMapping readAccessMapping() throws IOException {
        Path path = permissions.resolve("accessMapping.json");

        // Read the configuration
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                return Serialiser.deserialise(input, AccessMapping.class);
            }
        }

        // Or generate a new one:
        AccessMapping accessMapping = new AccessMapping();
        accessMapping.digitalPublishingTeam = new HashSet<>();
        accessMapping.paths = new HashMap<>();

        lock.readLock().lock();
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, accessMapping);
        } finally {
            lock.readLock().unlock();
        }

        return accessMapping;
    }

    private void writeAccessMapping(AccessMapping accessMapping) throws IOException {
        Path path = permissions.resolve("accessMapping.json");

        lock.writeLock().lock();
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, accessMapping);
        } finally {
            lock.writeLock().unlock();
        }
    }


}
