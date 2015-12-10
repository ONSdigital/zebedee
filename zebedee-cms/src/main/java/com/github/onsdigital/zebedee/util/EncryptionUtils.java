package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.cryptolite.Crypto;
import org.apache.commons.io.FileUtils;

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
     * @param outputStream any outputStream
     * @param key an encryption key
     * @return
     * @throws IOException
     */
    public static OutputStream encryptionOutputStream(OutputStream outputStream, SecretKey key) throws IOException {
        OutputStream cipherOutputStream = new Crypto().encrypt(outputStream, key);
        return cipherOutputStream;
    }
    /**
     * Get an output stream to write with encryption
     *
     * @param path a path to a file
     * @param key an encryption key
     * @return
     * @throws IOException
     */
    public static OutputStream encryptionOutputStream(Path path, SecretKey key) throws IOException {
        return encryptionOutputStream(FileUtils.openOutputStream(path.toFile()), key);
    }


    /**
     * Get an input stream to read with decryption
     *
     * @param inputStream any input stream
     * @param key a decryption key
     * @return
     * @throws IOException
     */
    public static InputStream encryptionInputStream(InputStream inputStream, SecretKey key) throws IOException {
        InputStream cipherInputStream = new Crypto().decrypt(inputStream, key);
        return cipherInputStream;
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
        return encryptionInputStream(Files.newInputStream(path), key);
    }
}
