package com.github.onsdigital.zebedee.keyring.cache;

import com.github.onsdigital.zebedee.keyring.KeyringException;

import javax.crypto.SecretKey;

/**
 * MigrationSchedulerKeyCache is a SchedulerKeyCache implementation that wraps around the legacy and
 * central caches allowing us to run them both in parallel so we can migrate smoothly to the new world.
 * <p>
 * If migration is enabled then MigrationSchedulerKeyCache will attempt to read/write from the new central scheduler
 * cache falling back on the legacy implementation if that is unsuccessful. If migration is not enabled - it
 * reads/writes to/from the legacy scheduler caches only
 * <p>
 * This is a temp class that will be deleted once the migration to new world keyring is complete.
 */
public class MigrationSchedulerKeyCache implements SchedulerKeyCache {

    private SchedulerKeyCache legacyCache;
    private SchedulerKeyCache centralCache;
    private boolean migrationEnabled;

    /**
     * Construct a new MigrationSchedulerKeyCache
     *
     * @param legacyCache      the legacy SchedulerKeyCache to wrap around.
     * @param centralCache     the central SchedulerKeyCache to use.
     * @param migrationEnabled enable/disable migration.
     */
    public MigrationSchedulerKeyCache(SchedulerKeyCache legacyCache, SchedulerKeyCache centralCache,
                                      boolean migrationEnabled) {
        this.legacyCache = legacyCache;
        this.centralCache = centralCache;
        this.migrationEnabled = migrationEnabled;
    }

    @Override
    public SecretKey get(String collectionID) throws KeyringException {
        if (migrationEnabled) {
            SecretKey key = centralCache.get(collectionID);
            if (key != null) {
                return key;
            }
        }
        return legacyCache.get(collectionID);
    }

    @Override
    public void add(String collectionID, SecretKey key) throws KeyringException {
        legacyCache.add(collectionID, key);
        if (migrationEnabled) {
            centralCache.add(collectionID, key);
        }
    }
}
