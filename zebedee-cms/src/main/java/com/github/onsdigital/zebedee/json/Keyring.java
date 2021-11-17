

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

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

/**
 * Represents the encryption keys needed for a user account to access collections.
 */
public class Keyring implements Cloneable {

    // Key storage:
    private String privateKeySalt;
    private String privateKey;
    private String publicKey;

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
            error().logException(e, "Error changing keyring password");
            result = false;
        }

        return result;
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
}
