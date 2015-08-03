package com.github.onsdigital.zebedee.exceptions;

/**
 * Superclass that identifies exceptions thrown by Zebedee that require a particular status code to be set
 * (other than default {@value org.eclipse.jetty.http.HttpStatus#INTERNAL_SERVER_ERROR_500} in an HTTP response to an API request.
 */
public abstract class ZebedeeException extends Exception {

    /**
     * The HTTP status code that a subclass of this exception implies,
     * if thrown up to the API level:
     */
    public final int statusCode;

    public ZebedeeException(String message, int responseCode) {
        super(message);
        this.statusCode = responseCode;
    }
}
