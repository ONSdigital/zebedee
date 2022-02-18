package com.github.onsdigital.zebedee.reader.api.filter;

import com.github.davidcarboni.restolino.api.RequestHandler;
import com.github.davidcarboni.restolino.framework.ServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeExceptionWithData;
import com.github.onsdigital.zebedee.reader.api.bean.ServerResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;

/**
 * The error handler catches various exceptions and sets the response status code accordingly.
 */
public class ErrorHandler implements ServerError {

    @Override
    public ServerResponse handle(HttpServletRequest req, HttpServletResponse res, RequestHandler requestHandler, Throwable t) {

        // If it's an ZebedeeExceptionWithData subclass, set the status code and message
        if (t != null && ZebedeeExceptionWithData.class.isAssignableFrom(t.getClass())) {
            ZebedeeExceptionWithData exception = (ZebedeeExceptionWithData) t;
            res.setStatus(exception.statusCode);
            error().data("exception_status_code", exception.statusCode)
                    .logException(exception, "Zebedee Reader API error");
            return new ServerResponse(exception.getMessage(), exception.getData());
        }
        // If it's an ZebedeeException subclass, set the status code and message
        if (t != null && ZebedeeException.class.isAssignableFrom(t.getClass())) {
            ZebedeeException exception = (ZebedeeException) t;
            res.setStatus(exception.statusCode);
            error().data("exception_status_code", exception.statusCode)
                    .logException(exception, "Zebedee Reader API error");
            return new ServerResponse(exception.getMessage());
        }
        // Otherwise leave the default 500 response
        error().logException(t, "Internal Server Error");
        return new ServerResponse("Internal Server Error");
    }
}
