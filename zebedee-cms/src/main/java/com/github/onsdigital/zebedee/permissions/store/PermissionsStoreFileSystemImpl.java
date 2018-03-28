package com.github.onsdigital.zebedee.permissions.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * A File system implementation of {@link PermissionsStore}. Provides functionality for reading / writing
 * {@link AccessMapping} objects to/from json files on disk.
 */
public class PermissionsStoreFileSystemImpl implements PermissionsStore {

    static final String PERMISSIONS_FILE = "accessMapping.json";
    static final String DATA_VIS_KEY = "dataVisualisationPublishers";
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
    public static void initialisePermissions(Path accessMappingPath) throws IOException {
        Path jsonPath = accessMappingPath.resolve(PERMISSIONS_FILE);

        if (!Files.exists(jsonPath)) {
            logDebug("AccessMapping file does not yet exist. Empty one will be created.").log();

            jsonPath.toFile().createNewFile();
            try (OutputStream output = Files.newOutputStream(jsonPath)) {
                Serialiser.serialise(output, new AccessMapping());
            }
        } else {
            migrateDataVisUsers(jsonPath);
        }
    }

    /**
     * Temporary legacy migration. Transfer deprecated Data Visualisation Publisher users to Digital Publishing team.
     * Can be reomved once all users have been safely migrated.
     */
    private static void migrateDataVisUsers(Path p) throws IOException {
        Map<String, Object> accessMapping = Serialiser.deserialise(p, Map.class);
        if (accessMapping.containsKey(DATA_VIS_KEY)) {

            List<String> dataVisualisationPublishers = (List<String>) accessMapping.get(DATA_VIS_KEY);
            if (dataVisualisationPublishers != null && !dataVisualisationPublishers.isEmpty()) {

                logDebug("Migrating users from Data Visualisation team to Digital Publishing team.").log();

                List<String> digitalPublishingTeam = (List<String>) accessMapping.get(PUBLISHERS_KEY);
                List<String> administrators = (List<String>) accessMapping.get(ADMINS_KEY);
                Map<String, Set<Integer>> collections = (Map<String, Set<Integer>>) accessMapping.get(COLLECTIONS_KEY);

                AccessMapping updated = new AccessMapping();
                updated.getDigitalPublishingTeam().addAll(digitalPublishingTeam);

                dataVisualisationPublishers.stream()
                        .forEach(dataVisUser -> {
                            logDebug("Migrating user")
                                    .user(dataVisUser)
                                    .log();
                            updated.getDigitalPublishingTeam().add(dataVisUser);
                        });

                updated.getAdministrators().addAll(administrators);
                updated.setCollections(collections);

                try (OutputStream output = Files.newOutputStream(p)) {
                    Serialiser.serialise(output, updated);
                }
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
