

package com.github.onsdigital.zebedee.json;

import com.github.davidcarboni.cryptolite.KeyExchange;
import com.github.davidcarboni.cryptolite.KeyWrapper;
import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.cryptolite.Random;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Represents the encryption keys needed for a user account to access collections.
 */
public class Keyring implements Cloneable {

    public transient Map<String, SecretKey> keys = new ConcurrentHashMap<>();
    // Key storage:
    private String privateKeySalt;
    private String privateKey;
    private String publicKey;
    private Map<String, String> keyring = new ConcurrentHashMap<>();
    // Runtime cache of decrypted keys:
    private transient KeyPair keyPair;

    /**
     * Generates a new {@link KeyPair} and initialises an empty keyring.
     *
     * @param password The password to use for securing the {@link java.security.PrivateKey PrivateKey}.
     * @return An initialised {@link Keyring} instance.
     */
    public static Keyring generate(String password) {
        Keyring result = new Keyring();

        // Generate a key pair
        result.keyPair = Keys.newKeyPair();
        result.storeKeyPair(result.keyPair, password);

        // Initialise the keyring
        result.keyring = new HashMap<>();
        result.keys = new HashMap<>();

        return result;
    }

    public int size() {
        return keyring.size();
    }

    public Keyring emptyClone() {
        Keyring keyring = new Keyring();
        keyring.privateKey = this.privateKey;
        keyring.publicKey = this.publicKey;
        keyring.privateKeySalt = this.privateKeySalt;

        Map<String, String> clonedKeyring = new ConcurrentHashMap<>();
        keyring.keyring = clonedKeyring;

        return keyring;
    }

    @Override
    public Keyring clone() {
        Keyring keyring = new Keyring();
        keyring.privateKey = this.privateKey;
        keyring.publicKey = this.publicKey;
        keyring.privateKeySalt = this.privateKeySalt;

        Map<String, String> clonedKeyring = new ConcurrentHashMap<>();
        for (String key : this.keyring.keySet()) {
            clonedKeyring.put(key, this.keyring.get(key));
        }
        keyring.keyring = clonedKeyring;

        return keyring;
    }

    /**
     * Unwraps (decrypts) the private key so that keys in the keyring can be accessed.
     *
     * @param password The password for the key.
     */
    public boolean unlock(String password) {
        boolean result;

        try {
            keyPair = new KeyWrapper(password, privateKeySalt).unwrapKeyPair(privateKey, publicKey);
            result = true;
        } catch (IllegalArgumentException e) {
            // Seems the private key could not be unwrapped, so return false
            logError(e, "Error unlocking keyring").log();
            result = false;
        }

        return result;
    }

    /**
     * Changes the {@link java.security.PrivateKey PrivateKey} password.
     * NB it's not possible to "reset" a key password because you can't decrypt the key
     * without storing a copy of the original password somewhere.
     *
     * @param oldPassword The current password for the {@link java.security.PrivateKey PrivateKey}.
     * @param newPassword A new password for the {@link java.security.PrivateKey PrivateKey}.
     * @return If the password was successfully changed, true. Otherwise false.
     * If you get false, you'll need to use {@link #generate(String)} instead and resend keys to this keyring.
     */
    public boolean changePassword(String oldPassword, String newPassword) {
        boolean result;

        try {
            // Recover the keypair
            // We make a point of using the oldPassword to ensure it's valid
            KeyPair keyPair = new KeyWrapper(oldPassword, privateKeySalt).unwrapKeyPair(privateKey, publicKey);
            storeKeyPair(keyPair, newPassword);
            result = true;
        } catch (IllegalArgumentException e) {
            // Seems the private key could not be unwrapped, so return false
            logError(e, "Error changing keyring password").log();
            result = false;
        }

        return result;
    }

    public void put(String collectionId, SecretKey collectionKey) {

        // Encrypt the key for storage
        KeyExchange keyExchange = new KeyExchange();
        String encryptedKey = keyExchange.encryptKey(collectionKey, getPublicKey());
        keyring.put(collectionId, encryptedKey);

        // Cache the key for speed
        keys.put(collectionId, collectionKey);
    }

    public SecretKey get(String collectionId) {
        SecretKey result = keys.get(collectionId);

        if (result == null && keyPair != null) {
            try {
                // Attempt to decrypt the key
                result = new KeyExchange().decryptKey(keyring.get(collectionId), keyPair.getPrivate());
                if (result != null)
                    keys.put(collectionId, result);
            } catch (IllegalArgumentException e) {
                // Error decrypting key
                logError(e, "Error recovering encryption key for collection")
                        .addParameter("collectionId", collectionId).log();
            }
        }

        if (result == null) {
            logInfo("Keyring has not been unlocked, cannot recover encryption key")
                    .addParameter("collectionId", collectionId).log();
        }

        return result;
    }

    /**
     * Removes a key from the keyring (and the in-memory cache).
     *
     * @param collectionId The collection to remove the key for.
     */
    public void remove(String collectionId) {
        keys.remove(collectionId);
        keyring.remove(collectionId);
    }

    /**
     * Lists the collection IDs in the keyring.
     *
     * @return An unmodifiable set of the key identifiers in the keyring.
     */
    public Set<String> list() {
        return java.util.Collections.unmodifiableSet(keyring.keySet());
    }

    /**
     * @return The {@link PublicKey} for this keyring.
     */
    public PublicKey getPublicKey() {
        PublicKey result;
        if (keyPair != null) {
            // Return the cached copy
            result = keyPair.getPublic();
        } else {
            // Decode from serialised data
            // NB it's not necessary to unlock a keyring
            // to access the public key
            result = KeyWrapper.decodePublicKey(publicKey);
        }
        return result;
    }

    public boolean isUnlocked() {
        return keyPair != null;
    }

    /**
     * Stores the given keypair, securing the private key with the given password.
     * <ul>
     * <li>The private key is wrapped (encrypted) using the password and a random salt value to generate an AES wrap key.</li>
     * <li>The public key is encoded without encryption so that it can be recovered by any user.</li>
     * </ul>
     *
     * @param keyPair  The {@link KeyPair} to be stored.
     * @param password A password to secure the private key.
     */
    private void storeKeyPair(KeyPair keyPair, String password) {
        // Encrypt ("wrap") the private key and encode the public
        // so they can be safely and securely serialised to Json
        privateKeySalt = Random.salt();
        KeyWrapper keyWrapper = new KeyWrapper(password, privateKeySalt);
        privateKey = keyWrapper.wrapPrivateKey(keyPair.getPrivate());
        publicKey = KeyWrapper.encodePublicKey(keyPair.getPublic());
    }

    @Override
    public String toString() {
        return keyring.keySet().toString();
    }
}
