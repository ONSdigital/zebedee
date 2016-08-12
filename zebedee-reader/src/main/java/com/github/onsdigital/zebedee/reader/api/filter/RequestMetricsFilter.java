package com.github.onsdigital.zebedee.reader.api.filter;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.onsdigital.zebedee.util.mertics.model.RequestMetrics;
import com.github.onsdigital.zebedee.util.mertics.model.SplunkEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by dave on 8/8/16.
 */
public class RequestMetricsFilter implements Filter {

    @Override
    public boolean filter(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        SplunkEvent.storeRequestMetrics(new RequestMetrics(httpServletRequest));
        return true;
    }
}
