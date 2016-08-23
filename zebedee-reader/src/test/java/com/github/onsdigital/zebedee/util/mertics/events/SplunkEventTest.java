package com.github.onsdigital.zebedee.util.mertics.events;

import com.google.common.collect.ImmutableMap;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.util.Map;

import static com.github.onsdigital.zebedee.util.mertics.events.SplunkEvent.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by dave on 8/18/16.
 */
public class SplunkEventTest {


    @Test
    public void shouldReturnExpectedEvent() {
        Map<String, Object> fields = new ImmutableMap.Builder<String, Object>()
                .put(API_KEY, API_KEY)
                .put(REQUESTED_URI_KEY, REQUESTED_URI_KEY)
                .put(HTTP_METHOD_KEY, HTTP_METHOD_KEY)
                .put(INTERCEPT_TIME_KEY, new Long(0))
                .put(PING_TIME_KEY, new Long(0))
                .put(TIME_TAKEN_KEY, new Long(0))
                .put(COLLECTION_PUBLISH_TIME, new Long(0))
                .put(COLLECTION_ID, "12345")
                .put(METRICS_TYPE_KEY, MetricsType.REQUEST_TIME)
                .build();

        SplunkEvent expected = new SplunkEvent(fields);

        SplunkEvent actual = new SplunkEvent.Builder()
                .api(API_KEY)
                .requestedURI(REQUESTED_URI_KEY)
                .httpMethod(HTTP_METHOD_KEY)
                .interceptTime(0)
                .timeTaken(0)
                .pingTime(0)
                .collectionPublishTime(0)
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
                .collectionPublishTime(0)
                .build(MetricsType.REQUEST_TIME, API_KEY);

        assertThat("Splunk event not as expected.", expected, equalTo(actual));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfMetricsTypeIsNull() {
        new SplunkEvent.Builder()
                .api(API_KEY)
                .timeTaken(0)
                .collectionPublishTime(0)
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
