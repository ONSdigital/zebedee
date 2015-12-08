package com.github.onsdigital.zebedee.util;

import static org.junit.Assert.*;

import com.github.davidcarboni.cryptolite.Keys;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by thomasridd on 25/11/2015.
 */
public class EncryptionUtilsTest {
Path path;

    @Before
    public void setUp() throws Exception {
        path = Files.createTempFile("EncryptionUtilsTest","txt");
    }

    @After
    public void tearDown() throws Exception {
        Files.delete(path);
    }

    @Test
    public void outputStream_withSecretKey_doesWriteGobbledygook() throws IOException {
        // Given
        // a secret key and some text
        SecretKey key = Keys.newSecretKey();
        String plain = "lorem ipsum dolor sit amet";

        // When
        // we encrypt it
        IOUtils.copy(IOUtils.toInputStream(plain), EncryptionUtils.encryptionOutputStream(path, key));

        // Then
        // when we read it as plain
        byte[] encoded = Files.readAllBytes(path);
        String value = new String(encoded, "UTF8");
        System.out.println(value);

        assertNotNull(value);
        assertNotEquals(plain, value);
    }

    // Now we have established that is rubbish lets reverse the tables
    @Test
    public void inputStream_withSecretKey_deciphersGobbledygook() throws IOException {
        // Given
        // some text we have encrypted
        SecretKey key = Keys.newSecretKey();
        String plain = "lorem ipsum dolor sit amet";
        IOUtils.copy(IOUtils.toInputStream(plain), EncryptionUtils.encryptionOutputStream(path, key));

        // When
        // we decrypt
        StringWriter writer = new StringWriter();
        IOUtils.copy(EncryptionUtils.encryptionInputStream(path, key), writer, "UTF8");
        String value = writer.toString();

        // Then
        // when we read it as plain
        assertEquals(plain, value);
    }

}