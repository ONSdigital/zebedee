package com.github.onsdigital.zebedee.keyring.cache;

import com.github.onsdigital.zebedee.keyring.KeyringException;

import javax.crypto.SecretKey;

public class MigrationSchedulerKeyCache implements SchedulerKeyCache {

    private SchedulerKeyCache legacyCache;
    private SchedulerKeyCache centralCache;
    private boolean migrationEnabled;

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
