package com.github.onsdigital.zebedee.exceptions;

import org.eclipse.jetty.http.HttpStatus;

/**
 * Created by bren on 03/08/15.
 */
public class CollectionNotFoundException extends ZebedeeException {
    public CollectionNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND_404);
    }
}
