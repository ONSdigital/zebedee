package com.github.onsdigital.zebedee.session.service.exceptions;

public class SessionsVerificationException extends SessionsException {
    public SessionsVerificationException(String message) {
        this(message, null);
    }

    public SessionsVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
