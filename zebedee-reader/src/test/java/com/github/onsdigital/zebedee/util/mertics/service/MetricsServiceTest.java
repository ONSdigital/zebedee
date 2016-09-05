package com.github.onsdigital.zebedee.util.mertics.service;

import com.github.onsdigital.zebedee.util.mertics.AbstractMetricsTest;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test verfies {@link MetricsService#getInstance()} returns the expected implementation depending on what environment
 * variables are set.
 */
public class MetricsServiceTest extends AbstractMetricsTest {

    /**
     * No Splunk config set in system properties so expects Dummy impl.
     */
    @Test
    public void shouldReturnDummyInstance() {
        assertThat("Expected Dummy Service.", MetricsService.getInstance() instanceof DummyMetricsServiceImpl);
    }

    @Test
    public void shouldReturnSplunkInstance() {
        setSystemProperties();
        MetricsService actualService = MetricsService.getInstance();
        assertThat("Expected Dummy Service.", actualService instanceof SplunkMetricsServiceImpl);

        SplunkMetricsServiceImpl splunkMetricsService = (SplunkMetricsServiceImpl) actualService;
        assertThat("Incorrect client", splunkMetricsService.getHttpEventCollectorURI(), equalTo(getURI()));
    }

    @Override
    public void before() throws Exception {
        clearSystemProperties();
    }

    @Override
    public void after() throws Exception {
        MetricsService.reset();
        clearSystemProperties();
    }
}
