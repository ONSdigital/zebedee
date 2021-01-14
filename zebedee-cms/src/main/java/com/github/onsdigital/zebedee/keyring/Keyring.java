package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;

public interface Keyring {

    void add(final Collection collection, final SecretKey secretKey) throws KeyringException;

    SecretKey get(final Collection collection) throws KeyringException;

    void remove(final Collection collection) throws KeyringException;
}
