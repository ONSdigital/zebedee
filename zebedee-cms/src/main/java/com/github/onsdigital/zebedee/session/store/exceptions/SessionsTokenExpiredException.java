package com.github.onsdigital.exceptions;

public class SessionsTokenExpiredException extends RuntimeException {
    public SessionsTokenExpiredException(String message) {
        this(message, null);
    }

    public SessionsTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
