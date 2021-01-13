package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;
import java.util.concurrent.locks.ReentrantLock;

public class KeyringServiceImpl implements KeyringService {

    private transient ReentrantLock lock;
    private transient KeyringStore store;
    private transient Keyring cache;

    public KeyringServiceImpl(final KeyringStore store) {
        this.lock = new ReentrantLock();
        this.store = store;
        this.cache = new Keyring();
    }

    public KeyringServiceImpl(final KeyringStore store, Keyring cache) {
        this.lock = new ReentrantLock();
        this.store = store;
        this.cache = cache;
    }

    @Override
    public void add(Collection collection, SecretKey collectionKey) throws KeyringException {
        validateParam(collection);
        validateParam(collectionKey);

        addNewKeyToKeyring(collection, collectionKey);
    }

    private void addNewKeyToKeyring(Collection collection, SecretKey collectionKey) throws KeyringException {
        lock.lock();
        try {
            boolean writeSuccessful = store.save(collection, collectionKey);
            if (!writeSuccessful) {
                throw new KeyringException("updating keyring was unsuccessful", collection.getDescription().getId());
            }

            this.cache.add(collection.getDescription().getId(), collectionKey);
        } finally {
            lock.unlock();
        }
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
