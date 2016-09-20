package com.github.onsdigital.zebedee.util.mertics.events;

import com.github.davidcarboni.restolino.framework.HttpMethod;
import com.google.common.collect.ImmutableMap;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.API_KEY;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.COLLECTION_ID;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.COLLECTION_PUBLISH_TIME;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.EVENT_KEY;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.HTTP_METHOD_KEY;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.INTERCEPT_TIME_KEY;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.METRICS_TYPE_KEY;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.PING_TIME_KEY;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.REQUESTED_URI_KEY;
import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.TIME_TAKEN_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by dave on 8/18/16.
 */
public class SplunkEventTest {

    private static final String REQUESTED_URI = "/U/R/I";
    private static final String API = "/U";
    private static final String URI_PARAM = "uri";
    private static final String BABBAGE_URI = "/businessindustryandtrade/itandinternetindustry/bulletins";
    private static final String METHOD = HttpMethod.GET.toString();

    @Mock
    private HttpServletRequest requestMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(requestMock.getRequestURI())
                .thenReturn(REQUESTED_URI);
        when(requestMock.getParameter(URI_PARAM))
                .thenReturn(BABBAGE_URI);
        when(requestMock.getMethod())
                .thenReturn(METHOD);
    }

    @Test
    public void shouldReturnExpectedEvent() {
        Map<String, Object> fields = new ImmutableMap.Builder<String, Object>()
                .put(API_KEY, API)
                .put(REQUESTED_URI_KEY, BABBAGE_URI)
                .put(HTTP_METHOD_KEY, METHOD)
                .put(INTERCEPT_TIME_KEY, new Long(0))
                .put(PING_TIME_KEY, new Long(0))
                .put(TIME_TAKEN_KEY, new Long(0))
                .put(COLLECTION_PUBLISH_TIME, new Long(0))
                .put(COLLECTION_ID, "12345")
                .put(METRICS_TYPE_KEY, MetricsType.REQUEST_TIME)
                .build();

        SplunkEvent expected = new SplunkEvent(fields);

        SplunkEvent actual = new SplunkEvent.Builder()
                .request(requestMock)
                .interceptTime(0)
                .timeTaken(0)
                .pingTime(0)
                .collectionPublishTimeTaken(0)
                .collectionId("12345")
                .build(MetricsType.REQUEST_TIME);

        assertThat("Splunk event not as expected.", expected, equalTo(actual));
    }

    @Test
    public void shouldReturnExpectedEventWithFieldExcluded() {
        Map<String, Object> fields = new ImmutableMap.Builder<String, Object>()
                .put(TIME_TAKEN_KEY, new Long(0))
                .put(COLLECTION_PUBLISH_TIME, new Long(0))
                .put(METRICS_TYPE_KEY, MetricsType.REQUEST_TIME)
                .build();

        SplunkEvent expected = new SplunkEvent(fields);

        SplunkEvent actual = new SplunkEvent.Builder()
                .api(API_KEY)
                .timeTaken(0)
                .collectionPublishTimeTaken(0)
                .build(MetricsType.REQUEST_TIME, API_KEY);

        assertThat("Splunk event not as expected.", expected, equalTo(actual));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfMetricsTypeIsNull() {
        new SplunkEvent.Builder()
                .api(API_KEY)
                .timeTaken(0)
                .collectionPublishTimeTaken(0)
                .build(null, API_KEY);
    }

    @Test
    public void shouldReturnTheExpectedJson() throws Exception {
        Map<String, Object> fields = new ImmutableMap.Builder<String, Object>()
                .put(API_KEY, API_KEY)
                .put(TIME_TAKEN_KEY, new Long(0))
                .put(COLLECTION_PUBLISH_TIME, new Long(0))
                .put(METRICS_TYPE_KEY, MetricsType.REQUEST_TIME)
                .build();

        Map<String, Object> event = new ImmutableMap.Builder<String, Object>()
                .put(EVENT_KEY, fields)
                .build();

        String expectedJSON = new ObjectMapper().writeValueAsString(event);
        assertThat("Splunk event not as expected.", expectedJSON, equalTo(new SplunkEvent(fields).toJson()));
    }
}
