package com.github.onsdigital.zebedee.reader.api.filter;

import com.github.davidcarboni.restolino.framework.PostFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;

/**
 * Filter to log the result of a request upon completion.
 */
public class RequestCompleteFilter implements PostFilter {

    private static final String PING = "/ping";
    private static final String HEALTH = "/health";
    private static final String REQUEST_COMPLETE = "http request completed";

    @Override
    public void filter(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();

        if (!uri.startsWith(PING) && !uri.startsWith(HEALTH)) {
            info().endHTTP(request, response).log(REQUEST_COMPLETE);
        }
    }
}
