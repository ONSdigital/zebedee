package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.util.Set;

import static java.text.MessageFormat.format;

/**
 * KeyringMigrator serves 2 purposes:
 * <ul>
 *     <li>It uses a feature flag to determine which {@link Keyring} implementation to use (legacy or new central).</li>
 *     <li>If the central keyring feature is disabled keys are read from the legacy keyring but adds/removes are
 *     applied to both legacy and central keyrings. This enables us to migrate smoothly from the old keyring
 *     without losing any keys.</li>
 * </ul>
 */
public class KeyringMigratorImpl implements Keyring {

    static final String ERR_FMT = "{0}: Migration enabled: {1}";

    static final String POPULATE_FROM_USER_ERR = "error while attempting to populate keyring from user";
    static final String GET_KEY_ERR = "error getting key from keyring";
    static final String REMOVE_KEY_ERR = "error removing key from keyring";

    private boolean migrationEnabled;
    private Keyring legacyKeyring;
    private Keyring centralKeyring;

    /**
     * Construct a new instance of the Keyring.
     *
     * @param migrationEnabled if true uses the new central keyring implementation. Otherwise uses legacy keyring
     *                         implemenation for reads. (Writes/Deletes will be applied to both).
     * @param legacyKeyring    the legacy keyring implementation to use.
     * @param centralKeyring   the new central keyring implementation to use.
     */
    public KeyringMigratorImpl(final boolean migrationEnabled, final Keyring legacyKeyring, final Keyring centralKeyring) {
        this.migrationEnabled = migrationEnabled;
        this.legacyKeyring = legacyKeyring;
        this.centralKeyring = centralKeyring;
    }

    @Override
    public void populateFromUser(User user) throws KeyringException {
        try {
            legacyKeyring.populateFromUser(user);
            centralKeyring.populateFromUser(user);
        } catch (KeyringException ex) {
            throw wrappedKeyringException(ex, POPULATE_FROM_USER_ERR);
        }
    }

    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        try {
            return getKeyring().get(user, collection);
        } catch (KeyringException ex) {
            throw wrappedKeyringException(ex, GET_KEY_ERR);
        }
    }

    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        try {
            centralKeyring.remove(user, collection);
            legacyKeyring.remove(user, collection);
        } catch (KeyringException ex) {
            throw wrappedKeyringException(ex, REMOVE_KEY_ERR);
        }
    }

    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {
        // TODO
    }

    @Override
    public Set<String> list(User user) throws KeyringException {
        // TODO
        return null;
    }

    private Keyring getKeyring() {
        if (migrationEnabled) {
            return centralKeyring;
        }

        return legacyKeyring;
    }

    KeyringException wrappedKeyringException(KeyringException cause, String msg) {
        return new KeyringException( format(ERR_FMT, msg, migrationEnabled), cause);
    }
}
