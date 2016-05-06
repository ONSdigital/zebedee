package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.onsdigital.logging.util.RequestLogUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter to add the X-Request-Id and remote address to the {@link org.slf4j.MDC} for logging.
 */
public class MDCFilter implements Filter {

    @Override
    public boolean filter(HttpServletRequest req, HttpServletResponse res) {
        RequestLogUtil.extractDiagnosticContext(req);
        return true;
    }
}
