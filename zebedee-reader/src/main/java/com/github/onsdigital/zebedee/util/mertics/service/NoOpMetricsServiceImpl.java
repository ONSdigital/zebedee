package com.github.onsdigital.zebedee.util.mertics.service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;

public class NoOpMetricsServiceImpl extends MetricsService {

    @Override
    public void captureRequest(HttpServletRequest request) {
        info().data("uri", request.getRequestURI()).log("no-op metrics service captureRequest");
    }

    @Override
    public void captureRequestResponseTimeMetrics() {
        info().log("no-op metrics service captureRequestResponseTimeMetrics");
    }

    @Override
    public void captureErrorMetrics() {
        info().log("no-op metrics service captureErrorMetrics");
    }

    @Override
    public void capturePing(long ms) {
        info().data("milliseconds", ms).log("no-op metrics service capturePing");
    }

    @Override
    public void captureCollectionPublishMetrics(String collectionId, long timeTaken, int numberOfFiles, String collectionType, Date publishDate) {
        info().data("collectionID", collectionId)
                .data("duration", timeTaken)
                .data("number_of_files", numberOfFiles)
                .data("colleciton_type", collectionType)
                .data("publish_date", publishDate)
                .log("no-op metrics service capturePing");
    }
}