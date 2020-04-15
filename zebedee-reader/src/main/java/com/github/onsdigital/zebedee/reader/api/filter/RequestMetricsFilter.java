package com.github.onsdigital.zebedee.reader.api.filter;

import com.github.davidcarboni.restolino.framework.PreFilter;
import com.github.onsdigital.zebedee.util.mertics.service.MetricsService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;

/**
 * Created by dave on 8/8/16.
 */
public class RequestMetricsFilter implements PreFilter {

    private static final String IGNORE_METRICS_HEADER = "metrics-disabled";
    private static MetricsService metricsService = MetricsService.getInstance();

    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(request.getHeader(IGNORE_METRICS_HEADER))) {
            try {
                metricsService.captureRequest(request);
            } catch (Exception ex) {
                error().exception(ex).log("metric service capture request threw an error");
            }
        }
        return true;
    }
}
