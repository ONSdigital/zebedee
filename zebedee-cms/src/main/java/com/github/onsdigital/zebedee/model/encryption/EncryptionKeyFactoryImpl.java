package com.github.onsdigital.zebedee.model.encryption;

import com.github.davidcarboni.cryptolite.Keys;

import javax.crypto.SecretKey;

public class EncryptionKeyFactoryImpl implements EncryptionKeyFactory {

    @Override
    public SecretKey newCollectionKey() {
        return Keys.newSecretKey();
    }
}
