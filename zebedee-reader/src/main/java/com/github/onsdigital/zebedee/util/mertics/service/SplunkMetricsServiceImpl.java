package com.github.onsdigital.zebedee.util.mertics.service;

import com.github.davidcarboni.restolino.framework.HttpMethod;
import com.github.onsdigital.zebedee.util.mertics.service.client.SplunkClient;
import com.github.onsdigital.zebedee.util.mertics.model.PingEvent;
import com.github.onsdigital.zebedee.util.mertics.model.RequestMetrics;
import com.github.onsdigital.zebedee.util.mertics.model.SplunkEvent;
import com.github.onsdigital.zebedee.util.mertics.model.SplunkRequestMessage;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Splunk implementation of {@link MetricsService} providing API for recording request time, error rates, ping times
 * and publish time stats.
 */
public class SplunkMetricsServiceImpl extends MetricsService {

    protected static ExecutorService pool = Executors.newSingleThreadExecutor();
    protected static ThreadLocal<SplunkEvent> splunkEventThreadLocal = new ThreadLocal<>();

    private String httpEventCollectorURI;
    private SplunkClient splunkClient = null;

    public SplunkMetricsServiceImpl(SplunkClient splunkClient, String httpEventCollectorURI) {
        this.splunkClient = splunkClient;
        this.httpEventCollectorURI = httpEventCollectorURI;
    }

    @Override
    public void captureRequest(HttpServletRequest request) {
        splunkEventThreadLocal.set(new RequestMetrics(request));
    }

    @Override
    public void captureRequestResponseTimeMetrics() {
        RequestMetrics metrics = (RequestMetrics) splunkEventThreadLocal.get();
        if (metrics != null) {
            metrics.stopTimer();
            metrics.setStatsType(SplunkEvent.StatsType.REQUEST_TIME);
            splunkClient.send(httpEventCollectorURI, buildRequest(metrics));
        }
    }

    @Override
    public void captureErrorMetrics() {
        RequestMetrics metrics = (RequestMetrics) splunkEventThreadLocal.get();
        if (metrics != null) {
            metrics.stopTimer();
            metrics.setStatsType(SplunkEvent.StatsType.REQUEST_ERROR);
            splunkClient.send(httpEventCollectorURI, buildRequest(metrics));
        }
    }

    @Override
    public void capturePing(long ms) {
        splunkClient.send(httpEventCollectorURI, buildRequest(new PingEvent(ms)));
    }

    public SplunkRequestMessage buildRequest(SplunkEvent splunkEvent) {
        return new SplunkRequestMessage(HttpMethod.POST, splunkEvent);
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
