package com.github.onsdigital.zebedee.exceptions;

public class InternalServerError extends ZebedeeException {

    public InternalServerError(String message, Throwable cause) {
        super(message, cause, 500);
    }
}
