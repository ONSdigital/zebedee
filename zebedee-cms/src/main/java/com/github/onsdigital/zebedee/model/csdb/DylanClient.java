package com.github.onsdigital.zebedee.model.csdb;

import java.io.IOException;
import java.io.InputStream;

/**
 * Client to send requests to Dylan
 */
public interface DylanClient {

    /**
     * Get the encrypted secret key from Dylan. The key will have been encrypted using the public key
     * provided by Zebedee - so must be decrypted using the private key held by zebedee.
     *
     * @return
     * @throws IOException
     */
    String getEncryptedSecretKey(String keyName) throws IOException;

    /**
     * Get the encrypted CSDB data from Dylan. The data will be encrypted using the secret key provided from dylan.
     * The getEncryptedSecretKey() method should be used to get the key to unlock this data.
     *
     * @param csdbIdentifier
     * @return
     * @throws IOException
     */
    InputStream getEncryptedCsdbData(String csdbIdentifier) throws IOException;
}
