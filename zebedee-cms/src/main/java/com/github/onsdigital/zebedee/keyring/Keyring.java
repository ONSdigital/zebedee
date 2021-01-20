package com.github.onsdigital.zebedee.keyring;

import javax.crypto.SecretKey;

public interface Keyring {

    void add(final String collectionID, final SecretKey collectionKey) throws KeyringException;

    SecretKey get(final String collectionID) throws KeyringException;

    void remove(final String collectionID) throws KeyringException;
}
