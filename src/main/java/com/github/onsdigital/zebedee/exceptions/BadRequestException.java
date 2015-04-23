package com.github.onsdigital.zebedee.exceptions;

/**
 * Created by david on 23/04/15.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
