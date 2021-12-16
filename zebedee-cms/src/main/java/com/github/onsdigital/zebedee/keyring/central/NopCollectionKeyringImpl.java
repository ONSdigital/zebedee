package com.github.onsdigital.zebedee.keyring.central;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * No-op implementation of the CollectionKeyring interface - empty keyring stubbed placeholder for the new cenral
 * keyring. DO NOTHING JUST LOOK PRETTY.
 */
public class NopCollectionKeyringImpl implements CollectionKeyring {

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
        return new HashSet<>();
    }
}