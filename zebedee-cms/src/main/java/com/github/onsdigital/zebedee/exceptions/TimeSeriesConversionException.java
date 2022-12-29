package com.github.onsdigital.zebedee.exceptions;

import org.eclipse.jetty.http.HttpStatus;

/**
 * {@link ZebedeeException} implementation for Errors caused by in invalid time series file sent to brian
 */
public class TimeSeriesConversionException extends ZebedeeException {

    public TimeSeriesConversionException(String msg) {
        // 409 status because the client request is fine, it's just the underlying state of the collection is invalid
        // (ie. the resource is in conflict)
        super(msg, HttpStatus.CONFLICT_409);
    }
}
