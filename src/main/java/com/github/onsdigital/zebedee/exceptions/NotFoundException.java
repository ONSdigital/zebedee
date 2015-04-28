package com.github.onsdigital.zebedee.exceptions;

import org.eclipse.jetty.http.HttpStatus;

/**
 * Created by david on 23/04/15.
 */
public class NotFoundException extends ZebedeeException {
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND_404);
    }
}
