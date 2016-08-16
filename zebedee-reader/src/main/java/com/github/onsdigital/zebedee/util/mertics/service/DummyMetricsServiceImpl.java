package com.github.onsdigital.zebedee.util.mertics.service;

import javax.servlet.http.HttpServletRequest;

public class DummyMetricsServiceImpl extends MetricsService {

    @Override
    public void captureRequest(HttpServletRequest request) {
        // Do nothing.
    }

    @Override
    public void captureRequestResponseTimeMetrics() {
        // Do nothing.
    }

    @Override
    public void captureErrorMetrics() {
        // Do nothing.
    }

    @Override
    public void capturePing(long ms) {
        // Do nothing.
    }
}
