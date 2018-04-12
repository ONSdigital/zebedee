package com.github.onsdigital.zebedee.user.model;

import com.github.davidcarboni.cryptolite.Password;
import com.github.onsdigital.zebedee.json.Keyring;
import org.apache.commons.lang3.BooleanUtils;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

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
    private String verificationHash;
    private Boolean verificationRequired;
    private Keyring keyring;

    /**
     * Constructor for deserialisation.
     */
    public User() {
        // No initialisation
    }

    /**
     * Authenticates this user.
     * @param password The user's password.
     * @return If the given password can be verified against {@link #passwordHash}, true.
     */
    public boolean authenticate(String password) {
        if(verificationRequired != null && verificationRequired) return false;

        return Password.verify(password, passwordHash);
    }

    public boolean verify(String code) {
        if(!verificationRequired) return false;

        // TODO use hashed code
        //return Password.verify(code, verificationHash);
        return code.equals(verificationHash);
    }

    /**
     * Changes the user's password and the {@link #keyring} password.
     * @param oldPassword The user's current password.
     * @param newPassword The new password.
     * @return If the old password can be authenticated and the keyring password is successfully changed, true.
     */
    public boolean changePassword(String oldPassword, String newPassword) {
        if (verificationRequired != null && verificationRequired) {
            if (verify(oldPassword)) {
                verificationRequired = false;
                verificationHash = "";
                if (keyring == null) {
                    keyring = Keyring.generate(newPassword);
                } else if (!keyring.changePassword(oldPassword, newPassword)) {
                    logInfo("Unable to change keyring password").log();
                    return false;
                }
                passwordHash = Password.hash(newPassword);
                return true;
            }
            logInfo("verification failed").log();
            return false;
        }

        if (authenticate(oldPassword)) {
            if (keyring().changePassword(oldPassword, newPassword)) {
                passwordHash = Password.hash(newPassword);
            } else {
                logInfo("Unable to change keyring password").log();
                return false;
            }
        } else {
            logInfo("Could not authenticate with the old password").log();
            return false;
        }

        return true;
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

    public void setKeyring(Keyring keyring) {
        this.keyring = keyring;
    }

    public void setVerificationHash(String verificationHash) {
        this.verificationHash = verificationHash;
    }

    public Boolean getVerificationRequired() {
        return verificationRequired;
    }

    public void setVerificationRequired(Boolean verificationRequired) {
        this.verificationRequired = verificationRequired;
    }

    @Override
    public String toString() {
        return name + ", " + email + (BooleanUtils.isTrue(inactive) ? " (inactive)" : "");
    }
}
