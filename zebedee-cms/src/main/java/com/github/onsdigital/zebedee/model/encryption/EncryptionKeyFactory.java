package com.github.onsdigital.zebedee.model.encryption;

import javax.crypto.SecretKey;

public interface EncryptionKeyFactory {

    SecretKey newCollectionKey();
}
