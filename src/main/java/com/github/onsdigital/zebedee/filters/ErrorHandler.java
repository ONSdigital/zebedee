package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.api.RequestHandler;
import com.github.davidcarboni.restolino.framework.ServerError;
import com.github.onsdigital.zebedee.exceptions.ApiException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The error handler catches various exceptions and sets the response status code accordingly.
 */
public class ErrorHandler implements ServerError {
    @Override
    public String handle(HttpServletRequest req, HttpServletResponse res, RequestHandler requestHandler, Throwable t) {

        // If it's an ApiException subclass, set the status code and message
        if (t != null && ApiException.class.isAssignableFrom(t.getClass())) {
            ApiException exception = (ApiException) t;
            res.setStatus(exception.statusCode);
            return exception.getMessage();
        }

        // Otherwise leave the default 500 response
        return null;
    }
}
