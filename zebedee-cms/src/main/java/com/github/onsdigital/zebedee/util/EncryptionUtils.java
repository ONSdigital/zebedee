package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.cryptolite.Crypto;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by thomasridd on 25/11/2015.
 */
public class EncryptionUtils {

    /**
     * Get an output stream to write with encryption
     *
     * @param path a path to a file
     * @param key an encryption key
     * @return
     * @throws IOException
     */
    public static OutputStream encryptionOutputStream(Path path, SecretKey key) throws IOException {
        OutputStream pathOutputStream = Files.newOutputStream(path);
        OutputStream cipherOutputStream = new Crypto().encrypt(pathOutputStream, key);
        return cipherOutputStream;
    }

    /**
     * Get an input stream to read with decryption
     *
     * @param path a path to a file
     * @param key a decryption key
     * @return
     * @throws IOException
     */
    public static InputStream encryptionInputStream(Path path, SecretKey key) throws IOException {
        InputStream pathInputStream = Files.newInputStream(path);
        InputStream cipherInputStream = new Crypto().decrypt(pathInputStream, key);
        return cipherInputStream;
    }
}
