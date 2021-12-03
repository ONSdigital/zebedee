package com.github.onsdigital.zebedee.session.service.exceptions;

public class SessionsDecodeException extends SessionsException {
    public SessionsDecodeException(String message) {
        this(message, null);
    }
    public SessionsDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
