package com.github.onsdigital.zebedee.permissions.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * @deprecated the files-on-disk access mapping is deprecated and will be removed once the migration of policy management
 *             to the dp-permissions-api has been completed
 *
 * // TODO: remove this implementation once the authorisation migration to using the dp-permissions-api been completed
 */
public class PermissionsStoreFileSystemImpl implements PermissionsStore {

    static final String PERMISSIONS_FILE = "accessMapping.json";
    static final String PUBLISHERS_KEY = "digitalPublishingTeam";
    static final String ADMINS_KEY = "administrators";
    static final String COLLECTIONS_KEY = "collections";

    private Path accessMappingPath;
    private Path accessMappingFilePath;
    private ReadWriteLock accessMappingLock = new ReentrantReadWriteLock();

    /**
     * Check if an {@link AccessMapping} json file exists in the permissions directory. If not a new empty instance
     * will be serialized to json and written to the permissions directory.
     *
     * @param accessMappingPath the path of the accessMapping json file.
     * @throws IOException error while initializing.
     */
    public static void initialiseAccessMapping(Path accessMappingPath) throws IOException {
        Path jsonPath = accessMappingPath.resolve(PERMISSIONS_FILE);

        if (!Files.exists(jsonPath)) {
            info().log("AccessMapping file does not yet exist. Empty one will be created.");

            jsonPath.toFile().createNewFile();
            try (OutputStream output = Files.newOutputStream(jsonPath)) {
                Serialiser.serialise(output, new AccessMapping());
            }
        }
    }

    /**
     * Create a new instance of the File system permissions store.
     *
     * @param accessMappingPath the path of the permissions directory to use.
     */
    public PermissionsStoreFileSystemImpl(Path accessMappingPath) {
        this.accessMappingPath = accessMappingPath;
        this.accessMappingFilePath = this.accessMappingPath.resolve(PERMISSIONS_FILE);
    }

    @Override
    public AccessMapping getAccessMapping() throws IOException {
        AccessMapping result = null;

        if (Files.exists(accessMappingFilePath)) {

            // Read the configuration
            accessMappingLock.readLock().lock();
            try (InputStream input = Files.newInputStream(accessMappingFilePath)) {
                result = Serialiser.deserialise(input, AccessMapping.class);
            } finally {
                accessMappingLock.readLock().unlock();
            }

            // Initialise any missing objects:
            if (result.getAdministrators() == null) {
                result.setAdministrators(new HashSet<>());
            }
            if (result.getDigitalPublishingTeam() == null) {
                result.setDigitalPublishingTeam(new HashSet<>());
            }
            if (result.getCollections() == null) {
                result.setCollections(new HashMap<>());
            }

        } else {

            // Or generate a new one:
            result = new AccessMapping();
            result.setAdministrators(new HashSet<>());
            result.setDigitalPublishingTeam(new HashSet<>());
            result.setCollections(new HashMap<>());
            saveAccessMapping(result);
        }

        return result;
    }

    @Override
    public void saveAccessMapping(AccessMapping accessMapping) throws IOException {
        accessMappingLock.writeLock().lock();
        try (OutputStream output = Files.newOutputStream(accessMappingFilePath)) {
            Serialiser.serialise(output, accessMapping);
        } finally {
            accessMappingLock.writeLock().unlock();
        }
    }
}
