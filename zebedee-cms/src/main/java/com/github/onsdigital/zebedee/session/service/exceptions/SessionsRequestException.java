package com.github.onsdigital.zebedee.session.service.exceptions;

public class SessionsRequestException extends SessionsException {
    public SessionsRequestException(String message) {
        this(message, null);
    }

    public SessionsRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}