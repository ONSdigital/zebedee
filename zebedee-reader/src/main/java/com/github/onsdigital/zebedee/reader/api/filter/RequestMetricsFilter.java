package com.github.onsdigital.zebedee.reader.api.filter;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.onsdigital.zebedee.util.mertics.service.MetricsService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;

/**
 * Created by dave on 8/8/16.
 */
public class RequestMetricsFilter implements Filter {

    private static final String IGNORE_METRICS_HEADER = "metrics-disabled";
    private static MetricsService metricsService = MetricsService.getInstance();

    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(request.getHeader(IGNORE_METRICS_HEADER))) {
            try {
                metricsService.captureRequest(request);
            } catch (Exception ex) {
                logError(ex).log();
            }
        }
        return true;
    }
}
