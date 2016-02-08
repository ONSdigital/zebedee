package com.github.onsdigital.zebedee.json.encryption;

public class StoredKeyPair {
    public String encryptedPrivateKey;
    public String encodedPublicKey;

    public StoredKeyPair(String encryptedPrivateKey, String encodedPublicKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.encodedPublicKey = encodedPublicKey;
    }
}
