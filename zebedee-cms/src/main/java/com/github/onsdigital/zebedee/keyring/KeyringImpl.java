package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyringImpl implements Keyring {

    private transient SecretKey secretKey;
    private transient KeyringStore store;
    private Map<String, SecretKey> cache;

    public KeyringImpl(final KeyringStore store, final SecretKey secretKey) {
        this.store = store;
        this.secretKey = secretKey;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public void add(Collection collection, SecretKey collectionKey) throws KeyringException {
        validateParam(collection);
        validateParam(collectionKey);
    }

    @Override
    public SecretKey get(Collection collection) throws KeyringException {
        return null;
    }

    @Override
    public boolean remove(Collection collection) throws KeyringException {
        return false;
    }

    private void validateParam(Collection collection) throws KeyringException {
        if (collection == null) {
            throw new KeyringException("keyring.add requires collection but was null");
        }

        if (collection.getDescription() == null) {
            throw new KeyringException("keyring.add requires collection.description but was null");
        }
    }

    private void validateParam(SecretKey secretKey) throws KeyringException {
        if (secretKey == null) {
            throw new KeyringException("keyring.add requires secretKey but was null");
        }
    }
}
