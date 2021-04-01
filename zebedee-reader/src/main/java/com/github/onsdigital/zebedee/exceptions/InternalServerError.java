package com.github.onsdigital.zebedee.exceptions;

public class InternalServerError extends ZebedeeException {

    public InternalServerError(String message, Throwable cause) {
        super(message, cause, 500);
    }

    public InternalServerError(String message) {
        super(message, 500);
    }
}
