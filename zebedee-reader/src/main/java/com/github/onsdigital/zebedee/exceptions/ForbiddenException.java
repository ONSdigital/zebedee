package com.github.onsdigital.zebedee.exceptions;

import org.eclipse.jetty.http.HttpStatus;

public class ForbiddenException extends ZebedeeException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN_403);
    }

}
