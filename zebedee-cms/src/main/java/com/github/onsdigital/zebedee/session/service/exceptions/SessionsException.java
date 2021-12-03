package com.github.onsdigital.zebedee.session.service.exceptions;

public class SessionsException extends RuntimeException {
    public SessionsException(String message) {
        this(message, null);
    }

    public SessionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
