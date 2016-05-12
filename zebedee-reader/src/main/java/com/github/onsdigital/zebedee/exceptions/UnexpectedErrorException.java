package com.github.onsdigital.zebedee.exceptions;

/**
 * A general {@Link ZebedeeException} implementation for unexpected errors.
 */
public class UnexpectedErrorException extends ZebedeeException {

    public UnexpectedErrorException(String message, int responseCode) {
        super(message, responseCode);
    }
}