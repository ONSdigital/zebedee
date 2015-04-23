package com.github.onsdigital.zebedee.exceptions;

/**
 * Created by david on 23/04/15.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
