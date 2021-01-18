package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.io.CollectionKeyReadWriter;
import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;

public class KeyringImpl implements Keyring {

    private final CollectionKeyReadWriter readWriter;

    public KeyringImpl(CollectionKeyReadWriter readWriter) {
        this.readWriter = readWriter;
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
