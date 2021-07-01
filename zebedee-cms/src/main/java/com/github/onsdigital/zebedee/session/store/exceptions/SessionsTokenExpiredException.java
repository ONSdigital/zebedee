package com.github.onsdigital.zebedee.session.store.exceptions;

public class SessionsTokenExpiredException extends SessionsStoreException {
    public SessionsTokenExpiredException(String message) {
        this(message, null);
    }

    public SessionsTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
