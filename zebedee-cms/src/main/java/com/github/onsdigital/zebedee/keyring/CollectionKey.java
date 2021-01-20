package com.github.onsdigital.zebedee.keyring;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.crypto.SecretKey;

/**
 * A collection key encapsulates a {@link SecretKey} used to encrypt collection content. Collection keys are
 * added/managed via the {@link Keyring}.
 */
public class CollectionKey {

    private String collectionID;
    private transient SecretKey secretKey;

    /**
     * Construct a new collection key.
     *
     * @param collectionID the ID of the collection the encryption key is for.
     * @param secretKey    the {@link SecretKey} used to encrypt the collection content.
     */
    public CollectionKey(final String collectionID, final SecretKey secretKey) {
        this.collectionID = collectionID;
        this.secretKey = secretKey;
    }

    /**
     * @return the collection ID the key belongs to.
     */
    public String getCollectionID() {
        return this.collectionID;
    }

    /**
     * @return the secret key used to encrypt/decrypt the collection content.
     */
    public SecretKey getSecretKey() {
        return this.secretKey;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || this.getClass() != o.getClass()) return false;

        final CollectionKey key = (CollectionKey) o;

        return new EqualsBuilder()
                .append(this.getCollectionID(), key.getCollectionID())
                .append(this.getSecretKey(), key.getSecretKey())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.getCollectionID())
                .append(this.getSecretKey())
                .toHashCode();
    }
}
