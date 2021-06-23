package com.github.onsdigital.zebedee.session.store.exceptions;

public class SessionsStoreException extends RuntimeException {
    public SessionsStoreException(String message) {
        this(message, null);
    }

    public SessionsStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
