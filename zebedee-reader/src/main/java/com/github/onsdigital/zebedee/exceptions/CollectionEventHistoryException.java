package com.github.onsdigital.zebedee.exceptions;

import javax.ws.rs.core.Response;

/**
 *
 */
public class CollectionEventHistoryException extends ZebedeeException {

    public CollectionEventHistoryException(String message) {
        super(message, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
