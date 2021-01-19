package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.io.CollectionKeyReadWriter;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class KeyringImpl implements Keyring {

    static final String INVALID_COLLECTION_ID_ERR_MSG = "expected collection ID but was null or empty";
    static final String INVALID_SECRET_KEY_ERR_MSG = "expected secret key but was null";
    static final String KEY_MISMATCH_ERR_MSG =
            "add unsuccessful a SecretKey with a different value is already associated with this collection ID";

    private CollectionKeyReadWriter collectionKeyReadWriter;
    private Map<String, CollectionKey> cache;
    private final ReentrantLock lock = new ReentrantLock();

    public KeyringImpl(final CollectionKeyReadWriter collectionKeyReadWriter) {
        this(collectionKeyReadWriter, new ConcurrentHashMap<>());
    }

    KeyringImpl(final CollectionKeyReadWriter collectionKeyReadWriter, final ConcurrentHashMap<String, CollectionKey> cache) {
        this.collectionKeyReadWriter = collectionKeyReadWriter;
        this.cache = cache;
    }

    @Override
    public void add(final String collectionID, final SecretKey secretKey) throws KeyringException {
        lock.lock();

        try {
            validateAddNewKeyParams(collectionID, secretKey);

            if (canAddKey(collectionID, secretKey)) {
                CollectionKey collectionKey = new CollectionKey(collectionID, secretKey);

                collectionKeyReadWriter.write(collectionKey);
                cache.put(collectionID, collectionKey);
            }
        } finally {
            lock.unlock();
        }
    }

    private void validateAddNewKeyParams(String collectionID, SecretKey secretKey) throws KeyringException {
        if (StringUtils.isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR_MSG);
        }

        if (secretKey == null) {
            throw new KeyringException(INVALID_SECRET_KEY_ERR_MSG);
        }
    }

    /**
     * Check if the a new {@link CollectionKey} for this collectionID can be added.
     *
     * @param collectionID
     * @param secretKey
     * @return True if no entry exists for this collectionID. False if there is an existing entry for the collection
     * ID and the {@link SecretKey} values match.
     * @throws KeyringException thrown if there is an existing entry for the collection ID but <b>the {@link SecretKey}
     *                          values do not match</b>. In this case we don't want to overrite the existing encryption
     *                          key value so bomb out with an error as something has gone wrong in the collection
     *                          creation process.
     */
    private boolean canAddKey(String collectionID, SecretKey secretKey) throws KeyringException {
        if (!cache.containsKey(collectionID)) {
            return true;
        }

        CollectionKey existingEntry = cache.get(collectionID);
        if (secretKey.equals(existingEntry.getSecretKey())) {
            return false;
        }

        throw new KeyringException(KEY_MISMATCH_ERR_MSG, collectionID);
    }

    @Override
    public SecretKey get(String collectionID) throws KeyringException {
        return null;
    }

    @Override
    public boolean remove(String collectionID) throws KeyringException {
        return false;
    }
}
