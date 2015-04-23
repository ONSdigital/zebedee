package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.api.RequestHandler;
import com.github.davidcarboni.restolino.framework.ServerError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The error handler catches various exceptions and sets the response status code accordingly.
 */
public class ErrorHandler implements ServerError {
    @Override
    public Object handle(HttpServletRequest req, HttpServletResponse res, RequestHandler requestHandler, Throwable t) {

        if (t != null && WebApplicationException.class.isAssignableFrom(t.getClass())) {

            WebApplicationException exception = (WebApplicationException) t;
            res.setStatus(exception.getResponse().getStatus());

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
