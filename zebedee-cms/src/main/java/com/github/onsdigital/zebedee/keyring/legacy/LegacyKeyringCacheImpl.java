package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.keyring.KeyringCache;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulate legacy scheduler cache implementation behind interface.
 */
public class LegacyKeyringCacheImpl implements KeyringCache {

    static final String COLLECTION_ID_EMPTY = "collection ID required but was null/empty";
    static final String SECRET_KEY_EMPTY = "secret key required but was null";

    private final ConcurrentHashMap<String, SecretKey> cache;

    /**
     * Construct a new SchedulerKeyringCache.
     *
     * @param cache the {@link ConcurrentHashMap} to use for the internal cache.
     */
    LegacyKeyringCacheImpl(final ConcurrentHashMap<String, SecretKey> cache) {
        this.cache = cache;
    }

    /**
     * Construct a new LegacySchedulerKeyringCache with the default configuration.
     */
    public LegacyKeyringCacheImpl() {
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

    @Override
    public void load() throws KeyringException {
        // Load only applies to new keyring implementation. Load should never be invoked unless the new keyring
        // feature flag is enabled - throw an exception here to flag up something is wrong.
        throw new UnsupportedOperationException("LegacyKeyringCacheImpl does not support load");
    }

    @Override
    public void remove(String collectionID) throws KeyringException {
        this.cache.remove(collectionID);
    }

    @Override
    public Set<String> list() throws KeyringException {
        if (cache == null) {
            return new HashSet<>();
        }

        return cache.keySet();
    }
}
