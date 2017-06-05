package com.github.onsdigital.zebedee.permissions.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.AccessMapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by dave on 31/05/2017.
 */
public class PermissionsStoreFileSystemImpl implements PermissionsStore {

    private static final String PERMISSIONS_FILE = "accessMapping.json";

    private Path accessMappingPath;
    private Path accessMappingFilePath;
    private ReadWriteLock accessMappingLock = new ReentrantReadWriteLock();

    public static void init(Path accessMappingPath) throws IOException {
        Path jsonPath = accessMappingPath.resolve(PERMISSIONS_FILE);

        if (!Files.exists(jsonPath)) {
            System.out.println("AccessMapping file does not yet exist. Empty one will be created.");

            jsonPath.toFile().createNewFile();

            try (OutputStream output = Files.newOutputStream(jsonPath)) {
                Serialiser.serialise(output, new AccessMapping());
            }
        }
    }

    public PermissionsStoreFileSystemImpl(Path accessMappingPath) {
        this.accessMappingPath = accessMappingPath;
        this.accessMappingFilePath = this.accessMappingPath.resolve(PERMISSIONS_FILE);

        if (!Files.exists(this.accessMappingFilePath)) {
            System.out.println("AccessMapping file does not yet exist. Empty one will be created.");

            accessMappingLock.writeLock().lock();
            try (OutputStream output = Files.newOutputStream(accessMappingFilePath)) {
                Serialiser.serialise(output, new AccessMapping());
            } catch (IOException e) {
                logError(e, "Unexpected error while attempting to create accessMapping file.").throwUnchecked(e);
            } finally {
                accessMappingLock.writeLock().unlock();
            }
        }
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
            if (result.getDataVisualisationPublishers() == null) {
                result.setDataVisualisationPublishers(new HashSet<>());
            }

        } else {

            // Or generate a new one:
            result = new AccessMapping();
            result.setAdministrators(new HashSet<>());
            result.setDigitalPublishingTeam(new HashSet<>());
            result.setCollections(new HashMap<>());
            result.setDataVisualisationPublishers(new HashSet<>());
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
