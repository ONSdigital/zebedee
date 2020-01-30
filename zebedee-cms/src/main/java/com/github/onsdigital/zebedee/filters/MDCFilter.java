package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.onsdigital.logging.util.RequestLogUtil;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Filter to add the X-Request-Id and remote address to the {@link org.slf4j.MDC} for logging.
 */
public class MDCFilter implements Filter {

    private static final String REQUEST_ID_KEY = "X-Request-Id";

    private static final String TRACE_ID_KEY = "trace_id";

    @Override
    public boolean filter(HttpServletRequest req, HttpServletResponse res) {
        MDC.put(TRACE_ID_KEY, getTraceID(req));
        RequestLogUtil.extractDiagnosticContext(req);
        return true;
    }

    private String getTraceID(HttpServletRequest req) {
        return defaultIfBlank(req.getHeader(REQUEST_ID_KEY), UUID.randomUUID().toString());
    }
}
