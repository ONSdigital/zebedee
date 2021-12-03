package com.github.onsdigital.zebedee.session.service.exceptions;

public class SessionsTokenExpiredException extends SessionsException {
    public SessionsTokenExpiredException(String message) {
        this(message, null);
    }

    public SessionsTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
