package com.github.onsdigital.zebedee.model.csdb;

import com.github.davidcarboni.cryptolite.Crypto;
import com.github.davidcarboni.cryptolite.KeyExchange;
import com.github.davidcarboni.cryptolite.Keys;
import org.apache.commons.io.IOUtils;

import javax.crypto.SecretKey;
import java.io.*;
import java.security.PublicKey;

/**
 * Instead of calling a remote dylan instance, just hold a secret key and return some test data encrypted.
 */
public class DummyDylanClient implements DylanClient {

    PublicKey publicKey;
    SecretKey secretKey;
    String testData = "this is test data instead of using actual csdb data";

    public DummyDylanClient(PublicKey publicKey) {
        this.publicKey = publicKey;
        this.secretKey = Keys.newSecretKey();
    }

    @Override
    public String getEncryptedSecretKey(String s) throws IOException {
        return new KeyExchange().encryptKey(secretKey, publicKey);
    }

    @Override
    public InputStream getEncryptedCsdbData(String csdbIdentifier) throws IOException {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream encryptedOutputStream = new Crypto().encrypt(outputStream, secretKey);

        IOUtils.copy(inputStream, encryptedOutputStream);

        return new ByteArrayInputStream(outputStream.toByteArray());

    }
}
