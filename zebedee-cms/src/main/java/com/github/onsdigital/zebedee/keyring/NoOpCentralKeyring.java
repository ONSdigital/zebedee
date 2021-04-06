package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * No-op implementation of the keyring interface - empty keyring stubbed placeholder for the new cenral keyring. DO
 * NOTHING JUST LOOK PRETTY.
 */
public class NoOpCentralKeyring implements Keyring {

    @Override
    public void cacheKeyring(User user) throws KeyringException {
        info().user(user.getEmail()).log("no-op keyring caching keyring");
    }

    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        info().user(user.getEmail()).collectionID(collection).log("no-op keyring get key");
        return null;
    }

    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        info().user(user.getEmail()).collectionID(collection).log("no-op keyring remove key");
    }

    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {
        info().user(user.getEmail()).collectionID(collection).log("no-op keyring add key");
    }

    @Override
    public Set<String> list(User user) throws KeyringException {
        info().user(user.getEmail()).log("no-op keyring list keys");
        return null;
    }

    @Override
    public void unlock(User user, String password) throws KeyringException {
        info().user(user.getEmail()).log("no-op keyring unlock");
    }

    @Override
    public void populate(User source, User target, Set<String> collectionIDs) throws KeyringException {

    }
}