package com.github.onsdigital.zebedee.util.mertics.service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class DummyMetricsServiceImpl extends MetricsService {


    @Override
    public void captureRequest(HttpServletRequest request) {

    }

    @Override
    public void captureRequestResponseTimeMetrics() {

    }

    @Override
    public void captureErrorMetrics() {

    }

    @Override
    public void capturePing(long ms) {

    }

    @Override
    public void captureCollectionsPublishTime(List<String> collectionIds, long publishTime) {

    }
}
