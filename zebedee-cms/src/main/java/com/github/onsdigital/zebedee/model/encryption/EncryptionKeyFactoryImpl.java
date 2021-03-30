package com.github.onsdigital.zebedee.model.encryption;

import com.github.davidcarboni.cryptolite.Keys;

import javax.crypto.SecretKey;

/**
 * Collection encryption key factory - encapsulates the creation of {@link SecretKey}'s behind an abstraction.
 * Decouples the code from a specific {@link SecretKey} provider and makes the code easier to test.
 */
public class EncryptionKeyFactoryImpl implements EncryptionKeyFactory {

    @Override
    public SecretKey newCollectionKey() {
        return Keys.newSecretKey();
    }
}
