package com.github.onsdigital.zebedee.session.store.exceptions;

public class SessionsDecodeException extends SessionsStoreException{
    public SessionsDecodeException(String message) {
        this(message, null);
    }
    public SessionsDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
