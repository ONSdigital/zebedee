package com.github.onsdigital.zebedee.keyring;

public class KeyNotFoundException extends KeyringException {

    public KeyNotFoundException(String message) {
        super(message);
    }

    public KeyNotFoundException(String message, String collectionID) {
        super(message, collectionID);
    }

    public KeyNotFoundException(String message, String collectionID, Throwable cause) {
        super(message, collectionID, cause);
    }

    public KeyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyNotFoundException(Throwable cause) {
        super(cause);
    }
}
