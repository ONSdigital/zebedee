package com.github.onsdigital.zebedee.session.service;

/**
 * A {@link RuntimeException} for use by the Sessions service when validation of a Session fails.
 */
public class SessionsException extends RuntimeException {

    /**
     * Construct a new instance of the exception
     *
     * @param message description of the error
     */
    public SessionsException(String message) {
        this(message, null);
    }

    /**
     * Construct a new instance of the exception
     *
     * @param message description of the error
     * @param cause   the cause of the exception
     */
    public SessionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
