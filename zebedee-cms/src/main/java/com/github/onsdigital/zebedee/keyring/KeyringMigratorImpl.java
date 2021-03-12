package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.util.Set;

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

    private boolean migrationEnabled;
    private Keyring legacyKeyring;
    private Keyring centralKeyring;

    public KeyringMigratorImpl(final boolean migrationEnabled, final Keyring legacyKeyring, final Keyring centralKeyring) {
        this.migrationEnabled = migrationEnabled;
        this.legacyKeyring = legacyKeyring;
        this.centralKeyring = centralKeyring;
    }

    @Override
    public void populateFromUser(User user) throws KeyringException {
        legacyKeyring.populateFromUser(user);
        centralKeyring.populateFromUser(user);
    }

    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        return getKeyring().get(user, collection);
    }

    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        // TODO
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
}
