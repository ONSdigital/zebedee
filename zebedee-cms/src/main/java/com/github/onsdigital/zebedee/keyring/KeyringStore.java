package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;

public interface KeyringStore {

    boolean save(Collection collection, SecretKey collectionKey) throws KeyringException;
}
