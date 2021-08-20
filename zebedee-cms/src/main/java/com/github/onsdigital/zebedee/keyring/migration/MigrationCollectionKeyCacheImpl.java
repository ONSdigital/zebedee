package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.KeyNotFoundException;
import com.github.onsdigital.zebedee.keyring.KeyringException;

import javax.crypto.SecretKey;
import java.util.Set;

/**
 * MigrationCollectionKeyCacheImpl is a CollectionKeyCache implementation that wraps around the legacy and
 * central caches allowing us to run them both in parallel so we can migrate smoothly to the new world.
 * <p>
 * If migration is enabled then MigrationCollectionKeyCache will attempt to read/write from the new central
 * collection key cache falling back on the legacy implementation if that is unsuccessful. If migration is not
 * enabled - it reads/writes to/from the legacy scheduler caches only
 * <p>
 * This is a temp class that will be deleted once the migration to new world keyring is complete.
 */
public class MigrationCollectionKeyCacheImpl implements CollectionKeyCache {

    private CollectionKeyCache legacyCache;
    private CollectionKeyCache newCache;
    private boolean migrationEnabled;

    /**
     * Construct a new MigrationCollectionKeyCacheImpl.
     *
     * @param legacyCache      the legacy {@link CollectionKeyCache} to wrap.
     * @param newCache         the new {@link CollectionKeyCache} to wrap.
     * @param migrationEnabled if true - reads/updates on both caches defaulting the new cache and falling back on
     *                         the legacy cache. If false only uses the legacy cache.
     */
    public MigrationCollectionKeyCacheImpl(CollectionKeyCache legacyCache, CollectionKeyCache newCache,
                                           boolean migrationEnabled) {
        this.legacyCache = legacyCache;
        this.newCache = newCache;
        this.migrationEnabled = migrationEnabled;
    }

    @Override
    public SecretKey get(String collectionID) throws KeyringException {
        if (migrationEnabled) {
            SecretKey key = getFromCentralKeyringCache(collectionID);
            if (key != null) {
                return key;
            }
        }
        return legacyCache.get(collectionID);
    }

    private SecretKey getFromCentralKeyringCache(String collectionID) throws KeyringException {
        try {
            return newCache.get(collectionID);
        } catch (KeyNotFoundException ex) {
            // keyringCache.get() throws an exception if the key is not found. In this use case key not found is a
            // valid response so we catch the specific exception class for this case and return null. Any other
            // keyring exception should be thrown & considered an error.
            return null;
        }
    }

    @Override
    public void add(String collectionID, SecretKey key) throws KeyringException {
        legacyCache.add(collectionID, key);

        if (migrationEnabled) {
            newCache.add(collectionID, key);
        }
    }

    @Override
    public void load() throws KeyringException {
        if (migrationEnabled) {
            newCache.load();
        }
    }

    @Override
    public void remove(String collectionID) throws KeyringException {
        legacyCache.remove(collectionID);
        if (migrationEnabled) {
            newCache.remove(collectionID);
        }
    }

    @Override
    public Set<String> list() throws KeyringException {
        if (migrationEnabled) {
            return newCache.list();
        }
        return legacyCache.list();
    }
}