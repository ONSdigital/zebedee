package com.github.onsdigital.zebedee.model.encryption;

import javax.crypto.SecretKey;

/**
 * Defines the behaviour of a collection encryption factory.
 */
public interface EncryptionKeyFactory {

    /**
     * Generate a new key.
     *
     * @return a new {@link SecretKey} instance.
     */
    SecretKey newCollectionKey();
}
