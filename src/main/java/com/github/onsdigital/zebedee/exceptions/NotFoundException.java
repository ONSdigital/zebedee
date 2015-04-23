package com.github.onsdigital.zebedee.exceptions;

/**
 * Created by david on 23/04/15.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
