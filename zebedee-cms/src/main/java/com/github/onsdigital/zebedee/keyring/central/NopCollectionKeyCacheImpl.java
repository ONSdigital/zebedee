package com.github.onsdigital.zebedee.keyring.central;

import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.KeyringException;

import javax.crypto.SecretKey;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * NoOp impl of {@link CollectionKeyCache}
 */
public class NopCollectionKeyCacheImpl implements CollectionKeyCache {
    @Override
    public void load() throws KeyringException {
        info().log("NopCollectionKeyCacheImpl: load");
    }

    @Override
    public void add(String collectionID, SecretKey collectionKey) throws KeyringException {
        info().collectionID(collectionID).log("NopCollectionKeyCacheImpl: add");
    }

    @Override
    public SecretKey get(String collectionID) throws KeyringException {
        info().collectionID(collectionID).log("NopCollectionKeyCacheImpl: get");
        return null;
    }

    @Override
    public void remove(String collectionID) throws KeyringException {
        info().collectionID(collectionID).log("NopCollectionKeyCacheImpl: remove");
    }

    @Override
    public Set<String> list() throws KeyringException {
        info().log("NopCollectionKeyCacheImpl: list");
        return null;
    }
}
