package com.github.onsdigital.zebedee.keyring.io;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import static com.github.onsdigital.zebedee.keyring.io.CollectionKeyStoreImpl.COLLECTION_KEY_ALREADY_EXISTS_ERR;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeyStoreImpl.COLLECTION_KEY_ID_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeyStoreImpl.COLLECTION_KEY_KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeyStoreImpl.COLLECTION_KEY_NOT_FOUND_ERR;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeyStoreImpl.COLLECTION_KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeyStoreImpl.INVALID_COLLECTION_ID_ERR;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeyStoreImpl.KEY_DECRYPTION_ERR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

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
    public void read_shouldThrowException_ifCollectionIDNull() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        CollectionKey key = null;
        try {
            key = store.read(null);
        } catch (KeyringException ex) {
            assertThat(key, is(nullValue()));
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void read_shouldThrowException_ifCollectionIDEmpty() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        CollectionKey key = null;
        try {
            key = store.read("");
        } catch (KeyringException ex) {
            assertThat(key, is(nullValue()));
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void read_shouldThrowException_ifKeyNotFound() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        CollectionKey key = null;
        try {
            key = store.read(TEST_COLLECTION_ID);
        } catch (KeyringException ex) {
            assertThat(key, is(nullValue()));
            assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_NOT_FOUND_ERR));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testRead_shouldThrowException_ifFileInvalid() throws Exception {
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec iv = createNewInitVector();

        SecretKey wrappedKey = createNewSecretKey();
        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID, wrappedKey);

        store = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, iv);

        CollectionKey actual = null;

        try {
            actual = store.read(TEST_COLLECTION_ID);
        } catch (KeyringException ex) {
            assertThat(actual, is(nullValue()));
            assertThat(ex.getMessage(), equalTo(KEY_DECRYPTION_ERR));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
    }

    @Test
    public void testRead_Success() throws Exception {
        // Create the key store
        SecretKey masterKey = createNewSecretKey();
        IvParameterSpec masterIV = createNewInitVector();
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), masterKey, masterIV);

        // Create a collection key to add to the keystore.
        IvParameterSpec initVector = createNewInitVector();
        CollectionKey collectionKey = new CollectionKey(TEST_COLLECTION_ID, createNewSecretKey());

        // Encrypt a test message using the collection key.
        String plainText = "Blackened is the end, Winter it will send, Throwing all you see Into obscurity";
        byte[] encryptedMessage = encyrpt(plainText, collectionKey.getSecretKey(), initVector);

        // write the key to the store.
        store.write(collectionKey);
        assertTrue(Files.exists(keyringDir.toPath().resolve(TEST_COLLECTION_ID + ".json")));

        // retieve the key from the store.
        CollectionKey actual = store.read(TEST_COLLECTION_ID);
        assertThat(actual.getCollectionID(), equalTo(TEST_COLLECTION_ID));

        // attempt to decrypt the test message using the key retrieve from the store - if its working as expected the
        // decrypted message should equal the original input.
        byte[] decryptedBytes = decrypt(encryptedMessage, collectionKey.getSecretKey(), initVector);
        String decryptedMessage = new String(decryptedBytes);
        assertThat(decryptedMessage, equalTo(plainText));
    }

    @Test(expected = KeyringException.class)
    public void testWrite_shouldThrowException_ifCollectionKeyIsNull() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.write(null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_NULL_ERR));
            assertThat(ex.getCollectionID(), is(nullValue()));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testWrite_shouldThrowException_ifCollectionKeyIDIsNull() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.write(new CollectionKey(null, null));
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_ID_NULL_ERR));
            assertThat(ex.getCollectionID(), is(nullValue()));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testWrite_shouldThrowException_ifCollectionKeySecretKeyIsNull() throws Exception {
        store = new CollectionKeyStoreImpl(keyringDir.toPath(), null, null);

        try {
            store.write(new CollectionKey(TEST_COLLECTION_ID, null));
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(COLLECTION_KEY_KEY_NULL_ERR));
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
        createPlainTextCollectionKeyFile(TEST_COLLECTION_ID, createNewSecretKey());

        try {
            // attempt to write another key with the same collection ID.
            store.write(new CollectionKey(TEST_COLLECTION_ID, createNewSecretKey()));
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
        CollectionKey input = new CollectionKey(TEST_COLLECTION_ID, createNewSecretKey());
        store.write(input);

        // check the file exists
        File f = keyringDir.toPath().resolve(TEST_COLLECTION_ID + ".json").toFile();
        assertTrue(Files.exists(f.toPath()));

        // Attempt to decrypt the file - if successful we can assume the encryption was also successful.
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, masterIV);

        CollectionKey result = null;
        try (
                FileInputStream fin = new FileInputStream(f);
                CipherInputStream cin = new CipherInputStream(fin, cipher);
                InputStreamReader reader = new InputStreamReader(cin)
        ) {
            result = new GsonBuilder()
                    .registerTypeAdapter(CollectionKey.class, new CollectionKeySerializer())
                    .create()
                    .fromJson(reader, CollectionKey.class);
        }

        // Verify the Collection key we created from decrypting the file and marshalling into the target object type
        // matches the origin input object.
        assertThat(result, equalTo(input));
    }

    @Test(expected = KeyringException.class)
    public void testDelete_collectionIDNull_shouldThrowException() throws Exception {
        store = new CollectionKeyStoreImpl(null, null, null);

        try {
            store.delete(null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR));
            throw ex;
        }
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

    byte[] encyrpt(String plainText, SecretKey key, IvParameterSpec iv) throws Exception {
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

    CollectionKey createPlainTextCollectionKeyFile(String collectionID, SecretKey secretKey) throws Exception {
        CollectionKey key = new CollectionKey(collectionID, secretKey);
        byte[] json = new GsonBuilder()
                .registerTypeAdapter(CollectionKey.class, new CollectionKeySerializer())
                .setPrettyPrinting()
                .create().toJson(key).getBytes();

        Path p = keyringDir.toPath().resolve(collectionID + ".json");

        FileUtils.writeByteArrayToFile(p.toFile(), json);

        return key;
    }
}
