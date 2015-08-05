package com.github.onsdigital.zebedee.exceptions;

import org.eclipse.jetty.http.HttpStatus;

/**
 * Created by david on 23/04/15.
 */
public class UnauthorizedException extends ZebedeeException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED_401);
    }

}
