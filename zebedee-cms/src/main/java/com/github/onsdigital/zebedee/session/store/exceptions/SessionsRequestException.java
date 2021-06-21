package com.github.onsdigital.zebedee.session.store.exceptions;

public class SessionsRequestException extends RuntimeException {
    public SessionsRequestException(String message) {
        this(message, null);
    }

    public SessionsRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}