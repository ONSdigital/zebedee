package com.github.onsdigital.zebedee.util.mertics.service;

import com.github.onsdigital.zebedee.Configuration;
import com.github.onsdigital.zebedee.util.mertics.AbstractMetricsTest;
import com.splunk.Args;
import org.junit.Ignore;
import org.junit.Test;

import java.text.MessageFormat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Verify the configuration util return the expected values based on the configuration and throws the expected error
 * when expected configuration is missing.
 */
public class SplunkConfigurationTest extends AbstractMetricsTest {

    @Override
    public void before() throws Exception {
        setSystemProperties();
    }

    @Override
    public void after() throws Exception {
        clearSystemProperties();
    }

    @Test
    public void shouldReturnSplunkEnabledTrue() {
        assertThat("Expected Splunk to be enabled.", Configuration.SplunkConfiguration.isSplunkEnabled(), is(true));
    }

    @Test
    public void shouldReturnSplunkServiceArgs() {
        Args expectedArgs = getTestServiceArgs();

        assertThat("Splunk Service Args not as expected.", Configuration.SplunkConfiguration.getServiceArgs(),
                equalTo(expectedArgs));
    }

    @Test
    public void shouldReturnSplunkEnabledFalse() {
        System.setProperty(SPLUNK_ENABLED_KEY, "false");
        assertThat("Expected Splunk to be disabled.", Configuration.SplunkConfiguration.isSplunkEnabled(), is(false));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForMissingSplunkTokenConfig() {
        testMissingConfig(() -> Configuration.SplunkConfiguration.getServiceArgs(), SPLUNK_TOKEN_KEY);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForMissingSplunkHostConfig() {
        testMissingConfig(() -> Configuration.SplunkConfiguration.getServiceArgs(), SPLUNK_HOST_KEY);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForMissingSplunkPortConfig() {
        testMissingConfig(() -> Configuration.SplunkConfiguration.getServiceArgs(), SPLUNK_PORT_KEY);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForMissingSplunk_HEC_URI_Config() {
        testMissingConfig(() -> Configuration.SplunkConfiguration.getEventsCollectionURI(), SPLUNK_HEC_URI_KEY);
    }

    private void testMissingConfig(Runnable getConfig, String key) {
        System.clearProperty(key);
        String expectedErrMsg = MessageFormat.format(Configuration.SplunkConfiguration.CONFIG_MISSING_MSG, key);

        try {
            getConfig.run();
        } catch (RuntimeException ex) {
            assertThat("Incorrect error message", ex.getMessage(), equalTo(expectedErrMsg));
            throw ex;
        }
    }
}
