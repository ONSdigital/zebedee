package com.github.onsdigital.zebedee.exceptions;

import java.util.Map;

/**
 * Superclass that identifies exceptions thrown by Zebedee that require a particular status code to be set
 * (other than default {@value org.eclipse.jetty.http.HttpStatus#INTERNAL_SERVER_ERROR_500} in an HTTP response to an API request.
 */
public abstract class ZebedeeExceptionWithData extends ZebedeeException {

    // Additional data which can be returned with the exception message
    protected Map<String, String> data;

    public ZebedeeExceptionWithData(String message, int responseCode) {
        super(message, responseCode);
    }

    public ZebedeeExceptionWithData(String message, int responseCode, Map<String, String> data) {
        super(message, responseCode);
        this.data = data;
    }

    public Map<String, String> getData() {
        return data;
    }
}
