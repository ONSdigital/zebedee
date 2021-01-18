package com.github.onsdigital.zebedee.keyring;

import javax.crypto.SecretKey;

public class KeyringImpl implements Keyring {
    @Override
    public void add(String collectionID, SecretKey collectionKey) throws KeyringException {

    }

    @Override
    public SecretKey get(String collectionID) throws KeyringException {
        return null;
    }

    @Override
    public boolean remove(String collectionID) throws KeyringException {
        return false;
    }
}
