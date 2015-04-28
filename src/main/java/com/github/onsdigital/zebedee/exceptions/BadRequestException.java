package com.github.onsdigital.zebedee.exceptions;

import org.eclipse.jetty.http.HttpStatus;

/**
 * Created by david on 23/04/15.
 */
public class BadRequestException extends ZebedeeException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST_400);
    }
}
