package com.github.onsdigital.zebedee.keyring;

import java.io.IOException;

import static java.text.MessageFormat.format;


public class KeyringException extends IOException {

    public static String formatErrorMesage(String message, String collectionID) {
        return format("{0} collectionID {1}", message, collectionID);
    }

    public KeyringException(final String message) {
        super(message);
    }

    public KeyringException(final String message, final String collectionID) {
        super(formatErrorMesage(message, collectionID));
    }

    public KeyringException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
