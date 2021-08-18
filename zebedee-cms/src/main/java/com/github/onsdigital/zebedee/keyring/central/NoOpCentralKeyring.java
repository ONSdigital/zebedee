package com.github.onsdigital.zebedee.keyring.central;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.util.List;
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
    public void assignTo(User src, User target, List<CollectionDescription> assignments) throws KeyringException {
        info().data("src", src.getEmail()).data("target", target.getEmail()).log("no-op assign keys to user");
    }

    @Override
    public void assignTo(User src, User target, CollectionDescription... assignments) throws KeyringException {
        info().data("src", src.getEmail()).data("target", target.getEmail()).log("no-op assign keys to user");
    }

    @Override
    public void revokeFrom(User target, List<CollectionDescription> removals) throws KeyringException {
        info().user(target.getEmail()).log("no-op revoke keys from user");
    }

    @Override
    public void revokeFrom(User target, CollectionDescription... removals) throws KeyringException {
        info().user(target.getEmail()).log("no-op revoke keys from user");
    }
}