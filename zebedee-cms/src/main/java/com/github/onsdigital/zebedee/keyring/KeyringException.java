package com.github.onsdigital.zebedee.keyring;

import java.io.IOException;

public class KeyringException extends IOException {

    public KeyringException(final String message) {
        super(message);
    }

    public KeyringException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
