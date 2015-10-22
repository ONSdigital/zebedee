package com.github.onsdigital.zebedee.json;

import com.github.davidcarboni.cryptolite.Password;
import com.github.onsdigital.zebedee.util.Log;
import org.apache.commons.lang3.BooleanUtils;

/**
 * Represents a user account. NB this record intentionally does not contain any permission-related information.
 * This is purely acconut information.
 * Created by david on 12/03/2015.
 */
public class User extends UserSanitised {

    // The password is used for both login and
    // to unlock the keyring, so we need to
    // manage these fields together.
    private String passwordHash;
    private Keyring keyring;

    /**
     * Constructor for deserialisation.
     */
    public User() {
        // No initialisation
    }

    /**
<<<<<<< HEAD
=======
     * Constructor for creating a new user.
     */
    public User(String password) {
        passwordHash = Password.hash(password);
        keyring = Keyring.generate(password);
    }

    /**
>>>>>>> Added changePassword and resetPassword to User so that a user's Keyring can be re-encrypted or re-initialised at the same time.
     * Authenticates this user.
     * @param password The user's password.
     * @return If the given password can be verified against {@link #passwordHash}, true.
     */
    public boolean authenticate(String password) {
        return Password.verify(password, passwordHash);
    }

    /**
     * Changes the user's password and the {@link #keyring} password.
     * @param oldPassword The user's current password.
     * @param newPassword The new password.
     * @return If the old password can be authenticated and the keyring password is successfully changed, true.
     */
    public boolean changePassword(String oldPassword, String newPassword) {
        boolean result = true;

        if (authenticate(oldPassword)) {
            if (keyring.changePassword(oldPassword, newPassword)) {
                passwordHash = Password.hash(newPassword);
                result = true;
            } else {
                Log.print("Unable to change keyring password.");
            }
        } else {
            Log.print("Could not authenticate with the old password.");
        }

        return result;
    }

    /**
     * Sets the user's password and generates a new, empty keyring.
     * @param password The new password for the user.
     */
    public void resetPassword(String password) {

        // Update the password hash
        passwordHash = Password.hash(password);

        // Generate a new key pair and wipe out any stored keys.
        // Without the original password none of the stored keys can be recovered.
        keyring = Keyring.generate(password);
    }

    /**
     * @return {@link #keyring}.
     */
    public Keyring keyring() {
        return keyring;
    }

    @Override
    public String toString() {
        return name + ", " + email + (BooleanUtils.isTrue(inactive) ? " (inactive)" : "");
    }
}
