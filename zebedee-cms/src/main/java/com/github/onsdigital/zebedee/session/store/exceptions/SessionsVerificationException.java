package com.github.onsdigital.zebedee.session.store.exceptions;

public class SessionsVerificationException extends SessionsStoreException {
    public SessionsVerificationException(String message) {
        this(message, null);
    }

    public SessionsVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
