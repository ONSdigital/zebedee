package com.github.onsdigital.zebedee.exceptions;

/**
 * Created by david on 28/04/2015.
 */
public abstract class ApiException extends RuntimeException {

    public final int statusCode;

    public ApiException(String message, int responseCode) {
        super(message);
        this.statusCode = responseCode;
    }
}
