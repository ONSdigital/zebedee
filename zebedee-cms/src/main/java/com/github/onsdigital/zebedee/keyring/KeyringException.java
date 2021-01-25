package com.github.onsdigital.zebedee.keyring;

import java.io.IOException;

/**
 * Exception type for errors while accessing/storing collection keys in the {@link Keyring}
 */
public class KeyringException extends IOException {

    private String collectionID;

    public KeyringException(final String message) {
        super(message);
    }

    public KeyringException(final String message, final String collectionID) {
        super(message);
        this.collectionID = collectionID;
    }

    public KeyringException(final String message, final String collectionID, final Throwable cause) {
        super(message, cause);
        this.collectionID = collectionID;
    }

    public KeyringException(final Throwable cause) {
        super(cause);
    }

    public String getCollectionID() {
        return this.collectionID;
    }
}
