package com.github.onsdigital.zebedee.util.mertics.service;

import com.github.davidcarboni.restolino.framework.HttpMethod;
import com.github.onsdigital.zebedee.util.mertics.client.SplunkClient;
import com.github.onsdigital.zebedee.util.mertics.client.SplunkRequest;
import com.github.onsdigital.zebedee.util.mertics.events.MetricsType;
import com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.INTERCEPT_TIME_KEY;

/**
 * Splunk implementation of {@link MetricsService} providing API for recording request time, error rates, ping times
 * and publish time stats.
 */
public class SplunkMetricsServiceImpl extends MetricsService {

    protected static ExecutorService pool = Executors.newSingleThreadExecutor();
    protected static ThreadLocal<SplunkEvent.Builder> splunkEventThreadLocal = new ThreadLocal<>();

    private String httpEventCollectorURI;
    private SplunkClient splunkClient = null;

    public SplunkMetricsServiceImpl(SplunkClient splunkClient, String httpEventCollectorURI) {
        this.splunkClient = splunkClient;
        this.httpEventCollectorURI = httpEventCollectorURI;
    }

    @Override
    public void captureRequest(HttpServletRequest request) {
        splunkEventThreadLocal.set(
                new SplunkEvent.Builder()
                        .interceptTime(System.currentTimeMillis())
                        .request(request));
    }

    @Override
    public void captureRequestResponseTimeMetrics() {
        SplunkEvent.Builder eventBuilder = splunkEventThreadLocal.get();
        if (eventBuilder != null) {
            long startTime = (Long) eventBuilder.get(INTERCEPT_TIME_KEY);
            eventBuilder.timeTaken(System.currentTimeMillis() - startTime);
            sendRequest(eventBuilder.build(MetricsType.REQUEST_TIME, INTERCEPT_TIME_KEY));
        }
    }

    @Override
    public void captureErrorMetrics() {
        SplunkEvent.Builder eventBuilder = splunkEventThreadLocal.get();
        if (eventBuilder != null) {
            long startTime = (Long) eventBuilder.get(INTERCEPT_TIME_KEY);
            eventBuilder.timeTaken(System.currentTimeMillis() - startTime);
            sendRequest(eventBuilder.build(MetricsType.REQUEST_ERROR, INTERCEPT_TIME_KEY));
        }
    }

    @Override
    public void capturePing(long ms) {
        sendRequest(new SplunkEvent.Builder()
                .pingTime(ms)
                .build(MetricsType.PING_TIME));
    }

    @Override
    public void captureCollectionPublishMetrics(String collectionId, long timeTaken, int numberOfFiles, String publishType, Date publishDate) {
        sendRequest(new SplunkEvent.Builder()
                .collectionId(collectionId)
                .collectionPublishTimeTaken(timeTaken)
                .collectionPublishFileCount(numberOfFiles)
                .collectionPublishType(publishType)
                .collectionPublishTime(publishDate)
                .build(MetricsType.COLLECTIONS_PUBLISH_TIME));
    }

    public void sendRequest(SplunkEvent splunkEvent) {
        try {
            splunkClient.send(httpEventCollectorURI, new SplunkRequest(HttpMethod.POST.name(), splunkEvent.toJson()));
        } catch (IOException ex) {
            logError(ex).log();
        }
    }

    public String getHttpEventCollectorURI() {
        return httpEventCollectorURI;
    }

    public SplunkClient getSplunkClient() {
        return splunkClient;
    }

    public void clearThreadLocal() {
        splunkEventThreadLocal.remove();
    }
}
