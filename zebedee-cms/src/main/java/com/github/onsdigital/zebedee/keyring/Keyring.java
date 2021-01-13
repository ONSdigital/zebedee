package com.github.onsdigital.zebedee.keyring;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Keyring {

    private Map<String, SecretKey> keys;

    Keyring() {
        this(new ConcurrentHashMap<>());
    }

    Keyring(final Map<String, SecretKey> keys) {
        this.keys = keys;
    }

    boolean isEmpty() {
        return keys.isEmpty();
    }

    void add(String collectionID, SecretKey collectionKey) {
        this.keys.put(collectionID, collectionKey);
    }

    SecretKey get(String collectionID) {
        return this.keys.get(collectionID);
    }
}
