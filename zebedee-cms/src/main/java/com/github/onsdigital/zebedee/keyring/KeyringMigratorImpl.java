package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.util.Set;

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
        return null;
    }

    @Override
    public void remove(User user, Collection collection) throws KeyringException {

    }

    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {

    }

    @Override
    public Set<String> list(User user) throws KeyringException {
        return null;
    }
}
