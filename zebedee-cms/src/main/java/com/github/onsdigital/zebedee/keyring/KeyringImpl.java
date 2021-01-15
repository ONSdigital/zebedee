package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.store.CollectionKeyStore;
import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;

public class KeyringImpl implements Keyring {

    private final CollectionKeyStore collectionKeyStore;

    public KeyringImpl(CollectionKeyStore collectionKeyStore) {
        this.collectionKeyStore = collectionKeyStore;
    }

    @Override
    public void add(Collection collection, SecretKey secretKey) throws KeyringException {

    }

    @Override
    public SecretKey get(Collection collection) throws KeyringException {
        return null;
    }

    @Override
    public void remove(Collection collection) throws KeyringException {

    }
}
