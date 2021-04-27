package com.github.onsdigital.zebedee.keyring.cache;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulate legacy scheduler cache implementation behind interface.
 */
public class LegacySchedulerKeyCache implements SchedulerKeyCache {

    static final String COLLECTION_ID_EMPTY = "collection ID required but was null/empty";
    static final String SECRET_KEY_EMPTY = "secret key required but was null";

    private final ConcurrentHashMap<String, SecretKey> cache;

    /**
     * Construct a new SchedulerKeyringCache.
     *
     * @param cache the {@link ConcurrentHashMap} to use for the internal cache.
     */
    LegacySchedulerKeyCache(final ConcurrentHashMap<String, SecretKey> cache) {
        this.cache = cache;
    }

    /**
     * Construct a new LegacySchedulerKeyringCache with the default configuration.
     */
    public LegacySchedulerKeyCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public SecretKey get(String collectionID) {
        if (cache == null) {
            return null;
        }

        return cache.get(collectionID);
    }

    @Override
    public void add(String collectionID, SecretKey key) throws KeyringException {
        if (StringUtils.isEmpty(collectionID)) {
            throw new KeyringException(COLLECTION_ID_EMPTY);
        }

        if (key == null) {
            throw new KeyringException(SECRET_KEY_EMPTY);
        }

        cache.put(collectionID, key);
    }
}
