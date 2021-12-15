package com.github.onsdigital.zebedee.user.model;

import com.github.davidcarboni.cryptolite.Password;
import com.github.onsdigital.zebedee.json.Keyring;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;

/**
 * Represents a user account. NB this record intentionally does not contain any permission-related information.
 * This is purely account information.
 * Created by david on 12/03/2015.
 */
public class User extends UserSanitised {

    // The password is used for both login and
    // to unlock the keyring, so we need to
    // manage these fields together.
    private String passwordHash;
    private Keyring keyring;

    private static final String UNSUPPORTED_METHOD = "unsupported attempt to call user password related method when JWT sessions are enabled";

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
     *
     * @deprecated the JWT session validation supersedes this functionality and this will method will be removed after
     *             the migration is complete.
     */
    @Deprecated
    public boolean authenticate(String password) {
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            error().log(UNSUPPORTED_METHOD);
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
        }

        return Password.verify(password, passwordHash);
    }

    /**
     * Changes the user's password and the {@link #keyring} password.
     * @param oldPassword The user's current password.
     * @param newPassword The new password.
     * @return If the old password can be authenticated and the keyring password is successfully changed, true.
     *
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    public boolean changePassword(String oldPassword, String newPassword) {
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            error().log(UNSUPPORTED_METHOD);
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
        }

        boolean result = false;
        if (authenticate(oldPassword)) {
            if (keyring.changePassword(oldPassword, newPassword)) {
                passwordHash = Password.hash(newPassword);
                result = true;
            } else {
                info().log("Unable to change keyring password");
            }
        } else {
            info().log("Could not authenticate with the old password");
        }

        return result;
    }

    /**
     * Sets the user's password and generates a new, empty keyring.
     * @param password The new password for the user.
     *
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    public void resetPassword(String password) {
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            error().log(UNSUPPORTED_METHOD);
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
        }

        // Update the password hash
        passwordHash = Password.hash(password);

        // Generate a new key pair and wipe out any stored keys.
        // Without the original password none of the stored keys can be recovered.
        keyring = Keyring.generate(password);
    }
}
