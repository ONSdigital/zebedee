package com.github.onsdigital.zebedee.keyring;

/**
 * Sub class implementation of {@link KeyringException} specifically for cases where the requested collection key is
 * not found.
 */
public class KeyNotFoundException extends KeyringException {

    /**
     * Construct a new instance of the exception.
     *
     * @param message      details about the context of the error
     * @param collectionID the ID of the collection that was not found.
     */
    public KeyNotFoundException(String message, String collectionID) {
        super(message, collectionID);
    }
}
