package com.github.onsdigital.zebedee.exceptions;

import javax.ws.rs.core.Response;

/**
 *
 */
public class CollectionAuditException extends ZebedeeException {

    public CollectionAuditException(String message) {
        super(message, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
