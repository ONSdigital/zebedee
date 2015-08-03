package com.github.onsdigital.zebedee.exceptions;

import org.eclipse.jetty.http.HttpStatus;

/**
 * Created by david on 23/04/15.
 */
public class ConflictException extends ZebedeeException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT_409);
    }
}
