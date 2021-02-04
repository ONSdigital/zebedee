package com.github.onsdigital.zebedee.keyring.store;

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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import static com.github.onsdigital.zebedee.keyring.store.CollectionKeyStoreImpl.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    @Test(expected = KeyringException.class)
    public void read_collectionIDNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.read(null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void read_collectionIDEmpty_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.read("");
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void read_keyNotFound_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.read(TEST_COLLECTION_ID);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_NOT_FOUND_ERR));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testRead_keyContentInvalid_shouldThrowException() throws Exception {
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec iv = createNewInitVector();

        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID);

        store = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, iv);

        try {
            store.read(TEST_COLLECTION_ID);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(KEY_DECRYPTION_ERR));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
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
        assertTrue(Files.exists(keyringDir.toPath().resolve(TEST_COLLECTION_ID + ".key")));

        // retrieve the key from the store.
        SecretKey keyReturned = store.read(TEST_COLLECTION_ID);

        // attempt to decrypt the test message using the key retrieved from the store - if its working as expected the
        // decrypted message should equal the original input.
        byte[] decryptedBytes = decrypt(encryptedMessage, keyReturned, initVector);
        String decryptedMessage = new String(decryptedBytes);
        assertThat(decryptedMessage, equalTo(plainText));
    }


    @Test(expected = KeyringException.class)
    public void testWrite_collectionIDNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.write(null, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            assertThat(ex.getCollectionID(), is(nullValue()));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testWrite_collectionIDEmpty_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.write("", null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            assertThat(ex.getCollectionID(), is(nullValue()));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testWrite_collectionKeyNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.write(TEST_COLLECTION_ID, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_NULL_ERR));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testWrite_shouldThrowException_ifCollectionKeyAlreadyExists() throws Exception {
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec masterIV = createNewInitVector();
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, masterIV);

        // create a plain key file in the keyring dir.
        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID);

        try {
            // attempt to write another key with the same collection ID.
            store.write(TEST_COLLECTION_ID, createNewSecretKey());
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_ALREADY_EXISTS_ERR));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
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
        File f = keyringDir.toPath().resolve(TEST_COLLECTION_ID + ".key").toFile();
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

    @Test(expected = KeyringException.class)
    public void testExists_collectionIDNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(null, null, null);

        try {
            store.exists(null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testExists_collectionIEmpty_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(null, null, null);

        try {
            store.exists("");
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            throw ex;
        }
    }

    @Test
    public void testExists_keyFileNotExist_shouldReturnFalse() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        assertFalse(store.exists(TEST_COLLECTION_ID));
    }

    @Test
    public void testExists_keyFileExist_shouldReturnTrue() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        Path p = Files.createFile(keyringDir.toPath().resolve(TEST_COLLECTION_ID + ".key"));
        assertTrue(Files.exists(p));

        assertTrue(store.exists(TEST_COLLECTION_ID));
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

        assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_NOT_FOUND_ERR));
        assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
    }

    @Test
    public void testDelete_success_shouldDeleteFile() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID);
        Path p = keyringDir.toPath().resolve(TEST_COLLECTION_ID + ".key");
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
        Path p = keyringDir.toPath().resolve(collectionID + ".key");
        FileUtils.writeByteArrayToFile(p.toFile(), "This is not a valid secret key".getBytes());
    }
}
