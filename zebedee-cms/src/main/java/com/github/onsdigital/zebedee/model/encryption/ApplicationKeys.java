package com.github.onsdigital.zebedee.model.encryption;

import com.github.davidcarboni.cryptolite.KeyWrapper;
import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.encryption.StoredApplicationKeys;
import com.github.onsdigital.zebedee.json.encryption.StoredKeyPair;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application keys are used for external application to send encrypted data to zebedee.
 * Zebedee provides the public keys for applications to send data to zebedee.
 * Only zebedee has the private keys to decrypt the data and use it.
 */
public class ApplicationKeys {

    public static final String FILENAME = "keys.json";

    private Path applicationKeysPath;
    private StoredApplicationKeys storedKeys;

    // in memory cache of unlocked keys. do not serialise!
    private transient Map<String, PrivateKey> privateKeyCache = new ConcurrentHashMap<>();
    private transient Map<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();

    /**
     * Create a new instance for given Path.
     *
     * @param applicationKeysPath
     */
    public ApplicationKeys(Path applicationKeysPath) {
        this.applicationKeysPath = applicationKeysPath;
    }

    /**
     * Generates a new key pair and secret key for the given application name.
     * The key pair is used to interact with the application.
     * The secret key is used to encrypt the private key.
     *
     * @param application
     * @return
     */
    public SecretKey generateNewKey(String application) throws IOException {

        // make new keys
        KeyPair keyPair = Keys.newKeyPair(); // the key pair allowing encrypted data to be passed to zebedee.
        SecretKey secretKey = Keys.newSecretKey(); // new secret key for encrypting the private key and storing on the users keyring.

        // encrypt and store the key pair.
        KeyWrapper keyWrapper = new KeyWrapper(secretKey);
        StoredKeyPair storedKeyPair = new StoredKeyPair(
                keyWrapper.wrapPrivateKey(keyPair.getPrivate()),
                keyWrapper.encodePublicKey(keyPair.getPublic()));

        // add the encrypted key pair to the map and save.
        StoredApplicationKeys applicationKeyPairs = getStoredKeys();
        applicationKeyPairs.put(application, storedKeyPair);
        write(applicationKeyPairs);

        // add the key pair to the in memory cache.
        privateKeyCache.put(application, keyPair.getPrivate());
        publicKeyCache.put(application, keyPair.getPublic());

        return secretKey;
    }

    /**
     * Return true if the given application has a key stored for it.
     *
     * @param application
     * @return
     */
    public boolean containsKey(String application) {
        return getStoredKeys().keySet().contains(application);
    }

    /**
     * Get the public encryption key for the given application identifier.
     *
     * @param application
     * @return
     */
    public PublicKey getPublicKey(String application) {
        // attempt to get it from the keyCache.
        if (publicKeyCache.containsKey(application))
            return publicKeyCache.get(application);

        // decode it from the key store and cache it.
        if (getStoredKeys().keySet().contains(application)) {
            String encodedPublicKey = getStoredKeys().get(application).encodedPublicKey;
            PublicKey publicKey = KeyWrapper.decodePublicKey(encodedPublicKey);
            publicKeyCache.put(application, publicKey);
            return publicKey;
        }

        return null;
    }

    /**
     * Get the public key for the given application ID, and then encode it.
     * @param applicationKeyId
     * @return
     */
    public String getEncodedPublicKey(String applicationKeyId) {
        return KeyWrapper.encodePublicKey(getPublicKey(applicationKeyId));
    }

    /**
     * Decrypt any missing keys using the given unlocked keyring.
     * @param keyring
     */
    public void populateCacheFromUserKeyring(Keyring keyring) {

        Set<String> keys = keyring.list();

        // decrypt and cache any keys that have not already been cached.
        for (String application : getStoredKeys().keySet()) {
            if (!isPrivateKeyCached(application) && keys.contains(application)) {
                SecretKey secretKey = keyring.get(application);

                StoredKeyPair storedKeyPair = getStoredKeys().get(application);

                KeyPair keyPair = new KeyWrapper(secretKey).unwrapKeyPair(
                        storedKeyPair.encryptedPrivateKey,
                        storedKeyPair.encodedPublicKey);

                publicKeyCache.put(application, keyPair.getPublic());
                privateKeyCache.put(application, keyPair.getPrivate());
            }
        }
    }

    /**
     * Get the private key for the given application if it has been decrypted / cached
     * @param application
     * @return
     */
    public PrivateKey getPrivateKeyFromCache(String application) {
        return privateKeyCache.get(application);
    }

    /**
     * Return true if the public key for the given application is in the cache.
     * @param application
     * @return
     */
    public boolean isPublicKeyCached(String application) {
        return publicKeyCache.containsKey(application);
    }

    /**
     * Return true if the private key for the given application is in the cache.
     * @param application
     * @return
     */
    public boolean isPrivateKeyCached(String application) {
        return privateKeyCache.containsKey(application);
    }

    /**
     * Read the stored key data from disk.
     * @return
     */
    private StoredApplicationKeys readKeys() {
        if (Files.exists(applicationKeysPath)) {
            try (InputStream input = Files.newInputStream(getFilePath())) {
                return Serialiser.deserialise(input, StoredApplicationKeys.class);
            } catch (IOException e) {
                // allow default return value
            }
        }

        return new StoredApplicationKeys();
    }

    /**
     * Write the given keys to disk.
     * @param applicationKeys
     * @throws IOException
     */
    private synchronized void write(StoredApplicationKeys applicationKeys) throws IOException {
        Files.createDirectories(applicationKeysPath);
        try (OutputStream output = Files.newOutputStream(getFilePath())) {
            Serialiser.serialise(output, applicationKeys);
        }
    }

    /**
     * Resolve the file path of the keys file.
     * @return
     */
    private Path getFilePath() {
        return applicationKeysPath.resolve(FILENAME);
    }

    /**
     * Getter for the stored keys - caches the keys in memory.
     * @return
     */
    private StoredApplicationKeys getStoredKeys() {
        if (storedKeys == null) {
            storedKeys = readKeys();
        }
        return storedKeys;
    }
}
