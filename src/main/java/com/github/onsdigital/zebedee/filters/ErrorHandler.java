package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.api.RequestHandler;
import com.github.davidcarboni.restolino.framework.ServerError;
import com.github.onsdigital.zebedee.exceptions.ApiException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The error handler catches various exceptions and sets the response status code accordingly.
 */
public class ErrorHandler implements ServerError {
    @Override
    public Object handle(HttpServletRequest req, HttpServletResponse res, RequestHandler requestHandler, Throwable t) {

        if (t != null && ApiException.class.isAssignableFrom(t.getClass())) {

            ApiException exception = (ApiException) t;
            res.setStatus(exception.statusCode);

            // Attempt to write out the exception message:
            try {
                PrintWriter writer = res.getWriter();
                writer.write(exception.getMessage());
            } catch (IOException e) {
                // Ignore - the response may have already been committed (or something else).
            }
        }

        // Otherwise leave the default 500 response code in place.
        return null;
    }
}
