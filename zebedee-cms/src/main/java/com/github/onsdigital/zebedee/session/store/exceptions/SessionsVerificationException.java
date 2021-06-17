package com.github.onsdigital.exceptions;

public class SessionsVerificationException extends RuntimeException {
    public SessionsVerificationException(String message) {
        this(message, null);
    }

    public SessionsVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
