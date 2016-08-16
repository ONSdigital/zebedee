package com.github.onsdigital.zebedee.util.mertics.service;

import com.github.davidcarboni.restolino.framework.HttpMethod;
import com.github.onsdigital.zebedee.util.mertics.AbstractMetricsTest;
import com.github.onsdigital.zebedee.util.mertics.model.PingEvent;
import com.github.onsdigital.zebedee.util.mertics.model.RequestMetrics;
import com.github.onsdigital.zebedee.util.mertics.model.SplunkEvent;
import com.github.onsdigital.zebedee.util.mertics.model.SplunkRequestMessage;
import com.github.onsdigital.zebedee.util.mertics.service.client.SplunkClient;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by dave on 8/15/16.
 */
public class SplunkMetricsServiceImplTest extends AbstractMetricsTest {

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private RequestMetrics metricsMock;

    @Mock
    private SplunkClient splunkClientMock;

    private SplunkMetricsServiceImpl metricsService;

    @Override
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        metricsService = new SplunkMetricsServiceImpl(splunkClientMock, SPLUNK_HEC_URI);
    }

    @Override
    public void after() throws Exception {
        metricsService.clearThreadLocal();
    }

    @Test
    public void shouldRecordPingData() {
        setSystemProperties();
        PingEvent pingEvent = new PingEvent(101);
        pingEvent.setStatsType(SplunkEvent.StatsType.PING_TIME);
        SplunkRequestMessage expectedSplunkRequest = new SplunkRequestMessage(HttpMethod.POST, pingEvent);

        metricsService.capturePing(pingEvent.getMs());

        verify(splunkClientMock, times(1)).send(SPLUNK_HEC_URI, expectedSplunkRequest);
    }

    @Test
    public void shouldRecordRequestTime() {
        when(requestMock.getMethod()).thenReturn(HttpMethod.POST.toString());
        when(requestMock.getRequestURI()).thenReturn("/data");
        when(requestMock.getParameter("uri")).thenReturn("/");

        RequestMetrics metrics = new RequestMetrics(requestMock);

        metricsService.captureRequest(requestMock);
        metricsService.captureRequestResponseTimeMetrics();
        metrics.setStatsType(SplunkEvent.StatsType.REQUEST_TIME);

        verify(splunkClientMock, times(1)).send(eq(SPLUNK_HEC_URI), any(SplunkRequestMessage.class));
    }

    @Test
    public void shouldNotSendSplunkRequest() {
        metricsService.captureRequestResponseTimeMetrics();
        verify(splunkClientMock, never()).send(anyString(), any(SplunkRequestMessage.class));
    }

    @Test
    public void testBuildRequest() {
        when(requestMock.getMethod()).thenReturn(HttpMethod.POST.toString());
        when(requestMock.getRequestURI()).thenReturn("/data");
        when(requestMock.getParameter("uri")).thenReturn("/");

        RequestMetrics metrics = new RequestMetrics(requestMock);

        assertThat("Splunk message request not as expected", new SplunkRequestMessage(HttpMethod.POST, metrics),
                equalTo(metricsService.buildRequest(metrics)));
    }

    @Test
    public void shouldRecordError() {
        when(requestMock.getMethod()).thenReturn(HttpMethod.POST.toString());
        when(requestMock.getRequestURI()).thenReturn("/data");
        when(requestMock.getParameter("uri")).thenReturn("/");

        metricsService.captureRequest(requestMock);
        metricsService.captureErrorMetrics();

        verify(splunkClientMock, times(1)).send(eq(SPLUNK_HEC_URI), any(SplunkRequestMessage.class));
    }

    @Test
    public void shouldNotRecordError() {
        metricsService.captureErrorMetrics();
        verify(splunkClientMock, never()).send(any(), any());
    }
}
