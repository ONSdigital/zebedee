package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.cryptolite.Crypto;
import org.apache.commons.io.FileUtils;

import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

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

    public static String createMD5Checksum(String value) {
        if (value == null){
            throw new IllegalArgumentException("Input value cannot be null");
        }

        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating MD5 checksum", e);
        }

        byte[] byteEncodedMessage = value.getBytes(UTF_8);
        byte[] md5Digest = messageDigest.digest(byteEncodedMessage);
        return DatatypeConverter.printHexBinary(md5Digest).toLowerCase();
    }
}
