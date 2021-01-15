package com.github.onsdigital.zebedee.keyring.store;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

public class CollectionKeyStoreImplTest {

    static final String TEST_COLLECTION_ID = "1234567890";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CollectionKeyStore keyStore;
    private File keyringDir;

    @Before
    public void setUp() throws Exception {
        keyringDir = folder.newFolder("keyring");
    }

    @Test(expected = KeyringException.class)
    public void read_shouldThrowException_ifCollectionIDNull() throws Exception {
        keyStore = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        CollectionKey key = null;
        try {
            key = keyStore.read(null);
        } catch (KeyringException ex) {
            assertThat(key, is(nullValue()));
            assertThat(ex.getMessage(), equalTo("collectionID required but was null or empty"));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void read_shouldThrowException_ifCollectionIDEmpty() throws Exception {
        keyStore = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        CollectionKey key = null;
        try {
            key = keyStore.read("");
        } catch (KeyringException ex) {
            assertThat(key, is(nullValue()));
            assertThat(ex.getMessage(), equalTo("collectionID required but was null or empty"));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void read_shouldThrowException_ifKeyNotFound() throws Exception {
        keyStore = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        CollectionKey key = null;
        try {
            key = keyStore.read(TEST_COLLECTION_ID);
        } catch (KeyringException ex) {
            assertThat(key, is(nullValue()));
            assertThat(ex.getMessage(), equalTo("collectionKey not found"));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testRead_shouldThrowException_ifFileInvalid() throws Exception {
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec iv = createNewInitVector();

        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID);

        keyStore = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, iv);

        CollectionKey actual = null;

        try {
            actual = keyStore.read(TEST_COLLECTION_ID);
        } catch (KeyringException ex) {
            assertThat(actual, is(nullValue()));
            assertThat(ex.getMessage(), equalTo("error while decrypting collectionKey file"));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
    }

    @Test
    public void testRead_shouldReturnCollectionKey() throws Exception {
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec masterIV = createNewInitVector();
        keyStore = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, masterIV);

        IvParameterSpec initVector = createNewInitVector();
        CollectionKey collectionKey = new CollectionKey(TEST_COLLECTION_ID, createNewSecretKey());

        String plainText = "Blackened is the end, Winter it will send, Throwing all you see Into obscurity";
        byte[] encryptedMessage = encyrpt(plainText, collectionKey.getSecretKey(), initVector);

        keyStore.write(collectionKey);
        assertTrue(Files.exists(keyringDir.toPath().resolve(TEST_COLLECTION_ID + ".json")));


        CollectionKey actual = keyStore.read(TEST_COLLECTION_ID);
        assertThat(actual.getCollectionID(), equalTo(TEST_COLLECTION_ID));

        String decryptedMessage = decrypt(encryptedMessage, collectionKey.getSecretKey(), initVector);
        assertThat(decryptedMessage, equalTo(plainText));
    }

    CollectionKey createPlainTextCollectionKeyFile(String collectionID) throws Exception {
        CollectionKey key = new CollectionKey(collectionID, null);
        byte[] json = new GsonBuilder().setPrettyPrinting().create().toJson(key).getBytes();

        Path p = keyringDir.toPath().resolve(collectionID + ".json");

        FileUtils.writeByteArrayToFile(p.toFile(), json);

        return key;
    }

    byte[] encyrpt(String plainText, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        return Base64.getEncoder().encode(cipher.doFinal(plainText.getBytes("UTF-8")));
    }

    String decrypt(byte[] encryptedMessage, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] bytes = Base64.getDecoder().decode(encryptedMessage);
        return new String(cipher.doFinal(bytes));
    }

    SecretKey createNewSecretKey() throws Exception {
        return KeyGenerator.getInstance("AES").generateKey();
    }

    IvParameterSpec createNewInitVector() {
        byte[] iv = new byte[16];

        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        return new IvParameterSpec(iv);
    }
}
