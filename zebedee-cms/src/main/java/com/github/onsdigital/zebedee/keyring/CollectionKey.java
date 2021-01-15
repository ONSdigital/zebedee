package com.github.onsdigital.zebedee.keyring;

import javax.crypto.SecretKey;

/**
 * Object defines an collection enctryption key item stored in the collection {@link Keyring}.
 */
public class CollectionKey {

    private String collectionID;
    private SecretKey secretKey;

    /**
     * Construct a new collectionkey.
     *
     * @param collectionID the ID of the collection the encryption key is for.
     * @param secretKey    the {@link SecretKey} used to encrypt the collection content.
     */
    public CollectionKey(final String collectionID, final SecretKey secretKey) {
        this.collectionID = collectionID;
        this.secretKey = secretKey;
    }

    /**
     *
     * @return the collection ID the key belongs to.
     */
    public String getCollectionID() {
        return this.collectionID;
    }

    /**
     * @return the secret key the used to encrypt/decrypt the collection content.
     */
    public SecretKey getSecretKey() {
        return this.secretKey;
    }
}
