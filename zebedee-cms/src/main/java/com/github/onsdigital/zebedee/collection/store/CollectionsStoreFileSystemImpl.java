package com.github.onsdigital.zebedee.collection.store;

import com.github.onsdigital.zebedee.collection.model.Collection;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.util.serialiser.JSONSerialiser;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CollectionsStoreFileSystemImpl implements CollectionsStore {

    private static final String JSON_EXT = ".json";
    static final String INVALID_COLLECTION_ID_ERR = "collectionID required but was null or empty";
    static final String NULL_COLLECTION_ERR = "collection required but was null";
    static final String COLLECTION_NOT_FOUND_ERR = "collection not found";

    private Path collectionsPath;
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private JSONSerialiser<Collection> collectionJSONSerialiser;

    public CollectionsStoreFileSystemImpl(Path collectionsPath) {
        this.collectionsPath = collectionsPath;
        this.collectionJSONSerialiser = new JSONSerialiser<>(Collection.class);
    }

    /**
     * Reads a list of all {@link Collection}s from disk.
     *
     * @return the list of {@link Collection}s
     * @throws IOException if a filesystem error occurs
     */
    @Override
    public Set<Collection> list() throws IOException {
        Set<Collection> result = new HashSet<>();

        readLock.lock();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collectionsPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path) || !path.endsWith(JSON_EXT)) {
                    continue;
                }

                try (InputStream input = Files.newInputStream(path)) {
                    Collection collection = collectionJSONSerialiser.deserialiseQuietly(input, path);

                    if (collection != null) {
                        result.add(collection);
                    }
                }
            }
        } finally {
            readLock.unlock();
        }
        return result;
    }

    /**
     * Reads the specified {@link Collection} from disk.
     *
     * @param collectionId the ID of the {@link Collection} to get
     * @return the requested {@link Collection}
     *
     * @throws IOException if a filesystem error occurs
     * @throws NotFoundException if the collection does not exist
     * @throws IllegalArgumentException if a null or empty collection ID is passed
     */
    @Override
    public Collection get(String collectionId) throws IOException, NotFoundException {
        if (StringUtils.isBlank(collectionId)) {
            throw new IllegalArgumentException(INVALID_COLLECTION_ID_ERR);
        }

        Collection result = null;

        readLock.lock();
        try {
            if (!exists(collectionId)) {
                throw new NotFoundException(COLLECTION_NOT_FOUND_ERR);
            }

            Path path = getCollectionPath(collectionId);
            try (InputStream input = Files.newInputStream(path)) {
                result = collectionJSONSerialiser.deserialiseQuietly(input, path);
            }
        } finally {
            readLock.unlock();
        }

        return result;
    }

    /**
     * Checks if a collection matching the specified ID exists on disk.
     *
     * @param collectionId The ID of the collection to check
     * @return <code>true</code> if the collection exists, <code>false</code> otherwise
     *
     * @throws IllegalArgumentException if a null or empty collection ID is passed
     */
    @Override
    public boolean exists(String collectionId) {
        if (StringUtils.isBlank(collectionId)) {
            throw new IllegalArgumentException(INVALID_COLLECTION_ID_ERR);
        }

        readLock.lock();
        try {
            Path path = getCollectionPath(collectionId);
            return path != null && Files.exists(path);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Saves the {@link Collection} to disk.
     *
     * @param collection the {@link Collection} to save
     *
     * @throws IOException if a filesystem error occurs
     * @throws NotFoundException if the collection does not exist
     * @throws IllegalArgumentException if a null {@link Collection} is passed or the {@link Collection}'s ID is null or empty
     */
    @Override
    public void save(Collection collection) throws IOException, NotFoundException {
        if (collection == null) {
            throw new IllegalArgumentException(NULL_COLLECTION_ERR);
        }

        if (StringUtils.isBlank(collection.getId())) {
            throw new IllegalArgumentException(INVALID_COLLECTION_ID_ERR);
        }

        Path path = getCollectionPath(collection.getId());

        writeLock.lock();
        try {
            if (!exists(collection.getId())) {
                throw new NotFoundException(COLLECTION_NOT_FOUND_ERR);
            }

            collectionJSONSerialiser.serialise(path, collection);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Deletes a {@link Collection} from disk.
     *
     * @param collectionId the ID of the {@link Collection} to delete
     *
     * @throws IOException if a filesystem error occurs
     * @throws NotFoundException if the collection does not exist
     */
    @Override
    public void delete(String collectionId) throws NotFoundException, IOException {
        if (StringUtils.isBlank(collectionId)) {
            throw new IllegalArgumentException(INVALID_COLLECTION_ID_ERR);
        }

        writeLock.lock();
        try {
            if (!exists(collectionId)) {
                throw new NotFoundException(COLLECTION_NOT_FOUND_ERR);
            }

            Files.delete(getCollectionPath(collectionId));
        } finally {
            writeLock.unlock();
        }
    }

    private Path getCollectionPath(String collectionId) {
        Path result = null;
        if (StringUtils.isNotBlank(collectionId)) {
            result = collectionsPath.resolve(PathUtils.toFilename(collectionId) + JSON_EXT);
        }
        return result;
    }
}
