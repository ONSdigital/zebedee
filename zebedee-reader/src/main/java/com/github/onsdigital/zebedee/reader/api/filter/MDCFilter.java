package com.github.onsdigital.zebedee.reader.api.filter;

import com.github.davidcarboni.restolino.framework.PreFilter;
import com.github.davidcarboni.restolino.framework.Priority;
import com.github.onsdigital.logging.util.RequestLogUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;

/**
 * Filter to add the X-Request-Id and remote address to the {@link org.slf4j.MDC} for logging.
 */
@Priority(1)
public class MDCFilter implements PreFilter {

    private static final String PING = "/ping";
    private static final String HEALTH = "/health";
    private static final String REQUEST_RECEIVED = "http request received";

    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();

        if (!uri.startsWith(PING) && !uri.startsWith(HEALTH)) {
            RequestLogUtil.extractDiagnosticContext(request);
            info().beginHTTP(request).log(REQUEST_RECEIVED);
        }

        return true;
    }
}
