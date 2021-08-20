package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.text.MessageFormat.format;

/**
 * Encapsulate legacy scheduler cache implementation behind interface.
 */
public class LegacySchedulerKeyCacheImpl implements CollectionKeyCache {

    static final String COLLECTION_ID_EMPTY = "collection ID required but was null/empty";
    static final String SECRET_KEY_EMPTY = "secret key required but was null";

    private final ConcurrentHashMap<String, SecretKey> cache;

    /**
     * Construct a new SchedulerKeyringCache.
     *
     * @param cache the {@link ConcurrentHashMap} to use for the internal cache.
     */
    LegacySchedulerKeyCacheImpl(final ConcurrentHashMap<String, SecretKey> cache) {
        this.cache = cache;
    }

    /**
     * Construct a new LegacySchedulerKeyringCache with the default configuration.
     */
    public LegacySchedulerKeyCacheImpl() {
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
    public void remove(String collectionID) throws KeyringException {
        if (cache != null) {
            cache.remove(collectionID);
        }
    }

    @Override
    public Set<String> list() throws KeyringException {
        if (cache == null) {
            return new HashSet<>();
        }
        return cache.keySet();
    }

    @Override
    public void load() throws KeyringException {
        // Load is required for the new keyring implementation but not supported by the legacy keyring functionality
        // and shouldn't be called. Throw exception here to flag up that it's being used when it shouldn't be.
        String msg = format("load functionality is not supported by {0}", this.getClass().getSimpleName());
        throw new UnsupportedOperationException(msg);
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
