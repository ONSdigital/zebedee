package com.github.onsdigital.zebedee.exceptions;

import com.github.onsdigital.zebedee.json.Session;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Created by david on 23/04/15.
 */
public class UnauthorizedException extends ZebedeeException {
    public UnauthorizedException(Session session) {
        this(session == null ? "Please log in" : "You do not have the right permission: " + session);
    }

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED_401);
    }
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED_401);
    }
}
