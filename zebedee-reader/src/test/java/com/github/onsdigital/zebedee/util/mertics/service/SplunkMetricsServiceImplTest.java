package com.github.onsdigital.zebedee.util.mertics.service;

import com.github.davidcarboni.restolino.framework.HttpMethod;
import com.github.onsdigital.zebedee.util.mertics.AbstractMetricsTest;
import com.github.onsdigital.zebedee.util.mertics.client.SplunkClient;
import com.github.onsdigital.zebedee.util.mertics.client.SplunkRequest;
import com.github.onsdigital.zebedee.util.mertics.events.MetricsType;
import com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent;
import com.google.common.collect.ImmutableMap;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests verify the behaviour of {@link SplunkMetricsServiceImpl} for cases where {@link } are available
 * and unavailable to record.
 */
public class SplunkMetricsServiceImplTest extends AbstractMetricsTest {

    @Mock
    private HttpServletRequest requestMock;

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
    public void shouldRecordPingData() throws Exception {
        setSystemProperties();
        String eventJSON = new SplunkEvent.Builder().addField("pingTime", 101).build(MetricsType.PING_TIME).toJson();
        SplunkRequest splunkRequest = new SplunkRequest("POST", eventJSON);

        metricsService.capturePing(101);

        verify(splunkClientMock, times(1)).send(SPLUNK_HEC_URI, splunkRequest);
    }

    @Test
    public void shouldRecordRequestTime() throws Exception {
        when(requestMock.getMethod()).thenReturn(HttpMethod.POST.toString());
        when(requestMock.getRequestURI()).thenReturn("/data");
        when(requestMock.getParameter("uri")).thenReturn("/");

        captureRequestAndAssertInteractions();
        captureXAndAssertInteractions(() -> metricsService.captureRequestResponseTimeMetrics());
        verify(splunkClientMock, times(1)).send(eq(SPLUNK_HEC_URI), any(SplunkRequest.class));
    }

    @Test
    public void shouldNotSendSplunkRequest() {
        metricsService.captureRequestResponseTimeMetrics();
        verify(splunkClientMock, never()).send(anyString(), any(SplunkRequest.class));
    }

    @Test
    public void shouldSendRequest() throws Exception {
        Map<String, Object> fields = new ImmutableMap.Builder<String, Object>().put("one", "one").build();
        Map<String, Object> event = new ImmutableMap.Builder<String, Object>().put("event", fields).build();

        SplunkEvent splunkEvent = new SplunkEvent(fields);
        String splunkEventJSON = new ObjectMapper().writeValueAsString(event);
        SplunkRequest expectedRequest = new SplunkRequest("POST", splunkEventJSON);

        metricsService.sendRequest(splunkEvent);

        verify(splunkClientMock, times(1)).send(SPLUNK_HEC_URI, expectedRequest);
    }

    @Test
    public void shouldRecordError() {
        when(requestMock.getMethod()).thenReturn(HttpMethod.POST.toString());
        when(requestMock.getRequestURI()).thenReturn("/data");
        when(requestMock.getParameter("uri")).thenReturn("/");

        captureRequestAndAssertInteractions();
        captureXAndAssertInteractions(() -> metricsService.captureErrorMetrics());

        verify(splunkClientMock, times(1)).send(eq(SPLUNK_HEC_URI), any(SplunkRequest.class));
    }

    @Test
    public void shouldNotRecordError() {
        metricsService.captureErrorMetrics();
        verify(splunkClientMock, never()).send(any(), any());
    }

    private void captureRequestAndAssertInteractions() {
        metricsService.captureRequest(requestMock);
        // Verify these methods a not called when capturing the request.
        verify(requestMock, never()).getRequestURI();
        verify(requestMock, never()).getParameter("uri");
        verify(requestMock, never()).getMethod();
    }

    private void captureXAndAssertInteractions(Runnable captureMetric) {
        captureMetric.run();
        verify(requestMock, times(2)).getRequestURI();
        verify(requestMock, times(1)).getParameter("uri");
        verify(requestMock, times(1)).getMethod();
    }
}
