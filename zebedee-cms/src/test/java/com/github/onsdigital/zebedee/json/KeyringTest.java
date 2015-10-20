package com.github.onsdigital.zebedee.json;

import com.github.davidcarboni.cryptolite.Crypto;
import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.httpino.Serialiser;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Tests for {@link Keyring}.
 */
public class KeyringTest {

    String password;
    Keyring generated;
    Keyring deserialised;

    String collectionId;
    SecretKey collectionKey;

    @Before
    public void setUp() throws Exception {

        // Set up the keypair
        password = Random.password(8);
        generated = Keyring.generate(password);

        // Store a key
        collectionId = Random.id();
        collectionKey = Keys.newSecretKey();
        generated.put(collectionId, collectionKey);

        // Serialise and deserialise again
        deserialised = serialiseDeserialise(generated);
    }

    /**
     * Should be able to unlock a keyring with the correct password.
     */
    @Test
    public void shouldUnlockKeypairWithCorrectPassword() {

        // Given
        // The deserialised keys
        Keyring keyring = deserialised;

        // When
        // We unlock the keys
        boolean result = keyring.unlock(password);

        // Then
        // The keys should be successfully recovered
        assertTrue(result);
    }

    /**
     * Shouldn't be able to unlock a keyring with an incorrect password.
     */
    @Test
    public void shouldNotUnlockKeypairWithIncorrectPassword() {

        // Given
        // The deserialised keys
        Keyring keyring = deserialised;

        // When
        // We unlock the keys
        boolean result = keyring.unlock(password + "incorrect");

        // Then
        // The keys should be successfully recovered
        // and no exception should be passed through when failing to unwrap the private key
        assertFalse(result);
    }

    /**
     * Requesting a key from an unlocked keyring should be ok.
     */
    @Test
    public void shouldRecoverKeyFromUnlockedKeyring() {

        // Given
        // An unlocked keyring
        Keyring keyring = deserialised;
        keyring.unlock(password);

        // When
        // We recover the key
        SecretKey key = keyring.get(collectionId);

        // Then
        // The keys should be successfully recovered
        assertNotNull(key);
        assertTrue(checkKey(collectionKey, key));
    }

    /**
     * Requesting a key from a locked keyring should return null.
     */
    @Test
    public void shouldNotRecoverKeyFromLockedKeyring() {

        // Given
        // A locked keyring
        Keyring keyring = deserialised;

        // When
        // We attempt to recover the key
        SecretKey key = keyring.get(collectionId);

        // Then
        // The key is not recovered and
        // we don't get an exception
        assertNull(key);
    }

    /**
     * Requesting a nonexistent key should return null.
     */
    @Test
    public void shouldNotRecoverNonexistentKeyFromKeyring() {

        // Given
        // An unlocked keyring
        Keyring keyring = deserialised;
        keyring.unlock(password);

        // When
        // We try to access a nonexistent key
        SecretKey key = keyring.get("Nonnexistent key");

        // Then
        // No key is returned and
        // we don't get an exception
        assertNull(key);
    }

    /**
     * Recovering a {@link SecretKey} after being cached in memory.
     */
    @Test
    public void shouldAddKeyToKeyringInMemory() {

        // Given
        // An unlocked keyring
        Keyring keyring = deserialised;
        keyring.unlock(password);

        // When
        // We add a key to the keyring
        SecretKey key = Keys.newSecretKey();
        keyring.put(collectionId, key);

        // Then
        // The key should be cached in memory
        SecretKey recoveredCache = keyring.get(collectionId);
        assertNotNull(recoveredCache);
        assertTrue(checkKey(key, recoveredCache));
    }


    /**
     * Recovering a {@link SecretKey} after being serialised and deserialised.
     */
    @Test
    public void shouldAddKeyToKeyringSerialised() {

        // Given
        // An unlocked keyring
        Keyring keyring = deserialised;
        keyring.unlock(password);

        // When
        // We add a key to the keyring
        SecretKey key = Keys.newSecretKey();
        keyring.put(collectionId, key);

        // Then
        // The added key should successfully serialise and deserialise
        keyring = serialiseDeserialise(keyring);
        keyring.unlock(password);
        SecretKey recoveredDeserialised = keyring.get(collectionId);
        assertNotNull(key);
        assertTrue(checkKey(key, recoveredDeserialised));
    }


    /**
     * Changing a keyring password.
     */
    @Test
    public void shouldChangePassword() {

        // Given
        // A new password
        String newPassword = Random.password(9);
        String oldPassword = password;
        Keyring keyring = generated;

        // When
        // We change the password
        keyring.changePassword(oldPassword, newPassword);

        // Then
        // The new password should unlock the keyring
        keyring = serialiseDeserialise(keyring);
        boolean unlocked = keyring.unlock(newPassword);
        assertTrue(unlocked);
    }


    /**
     * Listing collection IDs in the keyring.
     */
    @Test
    public void shouldListKeys() {

        // Given
        // An additional key in the keyring
        String newCollectionId = Random.id();
        generated.put(newCollectionId, Keys.newSecretKey());

        // When
        // We change the password
        Set<String> ids = generated.list();

        // Then
        // The expected IDs should be present
        assertEquals(2, ids.size());
        assertTrue(ids.contains(newCollectionId));
        assertTrue(ids.contains(collectionId));
    }


    /**
     * Removing a key from the keyring.
     */
    @Test
    public void shouldRemoveKey() {

        // Given
        // An additional key in the keyring
        String newCollectionId = Random.id();
        generated.put(newCollectionId, Keys.newSecretKey());

        // When
        // We remove the original key
        generated.remove(collectionId);

        // Then
        // Only the new ID should be left
        Set<String> ids = generated.list();
        assertEquals(1, ids.size());
        assertTrue(ids.contains(newCollectionId));
        assertFalse(ids.contains(collectionId));
    }

    /**
     * Removing a key from the keyring.
     */
    @Test
    public void shouldNotStoreTransientFields() {

        // Given
        // Json for a keyring
        String json = Serialiser.serialise(generated);

        // When
        // We deserialise to the test class
        KeyringCheck keyringCheck = Serialiser.deserialise(json, KeyringCheck.class);

        // Then
        // The fields we expect to be transient should not be present
        assertNull(keyringCheck.keyPair);
        assertNull(keyringCheck.keys);
    }

    // --- Helpers --- //

    /**
     * Converts the given instance to Json and back again.
     * This clears cached fields and ensures that
     * serialisation works as expected.
     *
     * @param keyring The instance to be serialised and then deserialised.
     * @return The deserialised instance.
     */
    private static Keyring serialiseDeserialise(Keyring keyring) {
        String json = Serialiser.serialise(keyring);
        return Serialiser.deserialise(json, Keyring.class);
    }

    /**
     * Checks that two keys are the same by encrypting with one and decrypting with the other.
     *
     * @param keyExpected The expected key.
     * @param keyActual   The key to be compared to the expected key.
     * @return If the keys are the same (ie encryption with expected and decryption with actual recovers the original cleartext), true.
     */
    private boolean checkKey(SecretKey keyExpected, SecretKey keyActual) {
        String cleartext = Random.id();
        String encrypted = new Crypto().encrypt(cleartext, keyExpected);
        String decrypted = new Crypto().decrypt(encrypted, keyActual);
        return StringUtils.equals(cleartext, decrypted);
    }

    /**
     * Test class that allows us to verify that fields we expect to be transient are not stored.
     */
    public class KeyringCheck {
        public KeyPair keyPair;
        public Map<String, SecretKey> keys = new ConcurrentHashMap<>();
    }
}