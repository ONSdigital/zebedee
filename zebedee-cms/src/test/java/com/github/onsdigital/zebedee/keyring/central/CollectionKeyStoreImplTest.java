package com.github.onsdigital.zebedee.keyring.central;

import com.github.onsdigital.zebedee.keyring.CollectionKeyStore;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import static com.github.onsdigital.zebedee.keyring.KeyringException.formatExceptionMsg;
import static com.github.onsdigital.zebedee.keyring.central.CollectionKeyStoreImpl.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class CollectionKeyStoreImplTest {

    static final String TEST_COLLECTION_ID = "1234567890";
    static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CollectionKeyStore store;
    private File keyringDir;

    @Before
    public void setUp() throws Exception {
        keyringDir = folder.newFolder("keyring");
    }

    @Test
    public void read_collectionIDNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.read(null));

        assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
    }

    @Test
    public void read_collectionIDEmpty_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.read(""));

        assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
    }

    @Test
    public void read_keyNotFound_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.read(TEST_COLLECTION_ID));

        assertThat(ex.getMessage(), equalTo(formatExceptionMsg(COLLECTION_KEY_NOT_FOUND_ERR, TEST_COLLECTION_ID)));
        assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
    }

    @Test
    public void testRead_keyContentInvalid_shouldThrowException() throws Exception {
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec iv = createNewInitVector();

        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID);

        store = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, iv);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.read(TEST_COLLECTION_ID));

        assertThat(ex.getMessage(), equalTo(formatExceptionMsg(KEY_DECRYPTION_ERR, TEST_COLLECTION_ID)));
        assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
    }

    @Test
    public void testRead_success_shouldReturnDecryptedSecretKey() throws Exception {
        // Create the key store
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec masterIV = createNewInitVector();
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, masterIV);

        // Create a collection key to add to the keystore.
        IvParameterSpec initVector = createNewInitVector();
        SecretKey collectionKey = createNewSecretKey();

        // Encrypt a test message using the collection key.
        String plainText = "Blackened is the end, Winter it will send, Throwing all you see Into obscurity";
        byte[] encryptedMessage = encrypt(plainText, collectionKey, initVector);

        // write the key to the store.
        store.write(TEST_COLLECTION_ID, collectionKey);
        assertTrue(Files.exists(keyringDir.toPath().resolve(TEST_COLLECTION_ID + KEY_EXT)));

        // retrieve the key from the store.
        SecretKey keyReturned = store.read(TEST_COLLECTION_ID);

        // attempt to decrypt the test message using the key retrieved from the store - if its working as expected the
        // decrypted message should equal the original input.
        byte[] decryptedBytes = decrypt(encryptedMessage, keyReturned, initVector);
        String decryptedMessage = new String(decryptedBytes);
        assertThat(decryptedMessage, equalTo(plainText));
    }


    @Test
    public void testWrite_collectionIDNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.write(null, null));

        assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
        assertThat(ex.getCollectionID(), is(nullValue()));
    }

    @Test
    public void testWrite_collectionIDEmpty_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.write("", null));

        assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
        assertThat(ex.getCollectionID(), is(nullValue()));
    }

    @Test
    public void testWrite_collectionKeyNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.write(TEST_COLLECTION_ID, null));

        assertThat(ex.getMessage(), equalTo(formatExceptionMsg(COLLECTION_KEY_NULL_ERR, TEST_COLLECTION_ID)));
        assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
    }

    @Test
    public void testWrite_shouldThrowException_ifCollectionKeyAlreadyExists() throws Exception {
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec masterIV = createNewInitVector();
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, masterIV);

        // create a plain key file in the keyring dir.
        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> store.write(TEST_COLLECTION_ID, createNewSecretKey()));

        assertThat(ex.getMessage(), equalTo(formatExceptionMsg(KEY_ALREADY_EXISTS_ERR, TEST_COLLECTION_ID)));
        assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
    }

    @Test
    public void testWrite_success_shouldWriteKeyToEncryptedFile() throws Exception {
        // Create a collection key writer.
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec masterIV = createNewInitVector();
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, masterIV);

        // Create a new collection key and write it's to the target destination.
        SecretKey collectionKey = createNewSecretKey();
        store.write(TEST_COLLECTION_ID, collectionKey);

        // check the file exists
        File f = keyringDir.toPath().resolve(TEST_COLLECTION_ID + KEY_EXT).toFile();
        assertTrue(Files.exists(f.toPath()));

        // Attempt to decrypt the file - if successful we can assume the encryption was also successful.
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, masterIV);

        SecretKey actualKey = null;
        try (
                FileInputStream fin = new FileInputStream(f);
                CipherInputStream cin = new CipherInputStream(fin, cipher);
        ) {
            byte[] keyBytes = IOUtils.toByteArray(cin);
            actualKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
        }

        // Verify the Collection key we created from decrypting the file and marshalling into the target object type
        // matches the origin input object.
        assertThat(actualKey, equalTo(collectionKey));
    }

    @Test
    public void testExists_collectionIDNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(null, null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.exists(null));

        assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
    }

    @Test
    public void testExists_collectionIEmpty_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(null, null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.exists(""));

        assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
    }

    @Test
    public void testExists_keyFileNotExist_shouldReturnFalse() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        assertFalse(store.exists(TEST_COLLECTION_ID));
    }

    @Test
    public void testExists_keyFileExist_shouldReturnTrue() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        Path p = Files.createFile(keyringDir.toPath().resolve(TEST_COLLECTION_ID + KEY_EXT));
        assertTrue(Files.exists(p));

        assertTrue(store.exists(TEST_COLLECTION_ID));
    }

    @Test
    public void testReadAll_noKeyFiles_shouldReturnEmptyMap() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        Map<String, SecretKey> actual = store.readAll();

        assertTrue(actual.isEmpty());
    }

    @Test
    public void testReadAll_failToDecryptKey_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), createNewSecretKey(), createNewInitVector());

        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.readAll());
        assertThat(ex.getMessage(), equalTo(formatExceptionMsg(KEY_DECRYPTION_ERR, TEST_COLLECTION_ID)));
        assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
    }

    @Test
    public void testReadAll_secondKeyFailsToDecrypt_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), createNewSecretKey(), createNewInitVector());

        // write a valid encrypted secret key to the store.
        store.write(TEST_COLLECTION_ID, createNewSecretKey());

        // Write an invalid unencrypted key to the store.
        createPlainTextCollectionKeyFile("abc123");

        // When the unencrypted key file is read it should fail to decrypt and throw exception.
        KeyringException ex = assertThrows(KeyringException.class, () -> store.readAll());

        assertThat(ex.getMessage(), equalTo(formatExceptionMsg(KEY_DECRYPTION_ERR, "abc123")));
        assertThat(ex.getCollectionID(), equalTo("abc123"));
    }

    @Test
    public void testReadAll_success_shouldReturnMapOfDecryptedKeys() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), createNewSecretKey(), createNewInitVector());

        // write a valid encrypted secret key to the store.
        SecretKey expectedColKey = createNewSecretKey();
        store.write(TEST_COLLECTION_ID, expectedColKey);

        Map<String, SecretKey> actual = store.readAll();

        assertFalse(actual.isEmpty());
        assertThat(actual.size(), equalTo(1));
        assertTrue(actual.containsKey(TEST_COLLECTION_ID));
        assertThat(actual.get(TEST_COLLECTION_ID), equalTo(expectedColKey));
    }

    @Test
    public void testReadAll_keyringDirDoesNotExist_shouldThrowException() {
        store = new CollectionKeyStoreImpl(Paths.get("/nonexistantpath"), null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.readAll());

        assertThat(ex.getMessage(), equalTo("error reading collectionKey keyring dir not found"));
    }


    @Test
    public void testDelete_collectionIDNull_shouldThrowKeyringException() {
        store = new CollectionKeyStoreImpl(null, null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.delete(null));

        assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
    }

    @Test
    public void testDelete_collectionIDEmpty_shouldThrowKeyringException() {
        store = new CollectionKeyStoreImpl(null, null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.delete(""));

        assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
    }

    @Test
    public void testDelete_keyNotExists_shouldThrowException() {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        KeyringException ex = assertThrows(KeyringException.class, () -> store.delete(TEST_COLLECTION_ID));

        assertThat(ex.getMessage(), equalTo(formatExceptionMsg(COLLECTION_KEY_NOT_FOUND_ERR, TEST_COLLECTION_ID)));
        assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
    }

    @Test
    public void testDelete_success_shouldDeleteFile() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID);
        Path p = keyringDir.toPath().resolve(TEST_COLLECTION_ID + KEY_EXT);
        assertTrue(Files.exists(p));

        store.delete(TEST_COLLECTION_ID);

        assertFalse(Files.exists(p));
    }

    byte[] encrypt(String plainText, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        return Base64.getEncoder().encode(cipher.doFinal(plainText.getBytes("UTF-8")));
    }

    byte[] decrypt(byte[] encryptedMessage, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] bytes = Base64.getDecoder().decode(encryptedMessage);
        return cipher.doFinal(bytes);
    }

    void createPlainTextCollectionKeyFile(String collectionID) throws Exception {
        Path p = keyringDir.toPath().resolve(collectionID + KEY_EXT);
        FileUtils.writeByteArrayToFile(p.toFile(), "This is not a valid secret key".getBytes());
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
