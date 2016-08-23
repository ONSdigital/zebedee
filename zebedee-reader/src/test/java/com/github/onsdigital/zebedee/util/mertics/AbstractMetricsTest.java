package com.github.onsdigital.zebedee.util.mertics;

import com.splunk.ServiceArgs;
import org.junit.After;
import org.junit.Before;

/**
 * Created by dave on 8/16/16.
 */
public abstract class AbstractMetricsTest {

    public static final String SPLUNK_ENABLED_KEY = "enable_splunk_reporting";

    public static final String SPLUNK_TOKEN_KEY = "splunk_http_event_collection_auth_token";
    public static final String SPLUNK_TOKEN = "010353BA-2DD4-4AE4-B66A-FC88D976308A";

    public static final String SPLUNK_HOST_KEY = "splunk_http_event_collection_host";
    public static final String SPLUNK_HOST = "localhost";

    public static final String SPLUNK_PORT_KEY = "splunk_http_event_collection_port";
    public static final String SPLUNK_PORT = "8099";

    public static final String SPLUNK_HEC_URI_KEY = "splunk_http_event_collection_uri";
    public static final String SPLUNK_HEC_URI = "/services/collector/event";

    public abstract void before() throws Exception;

    public abstract void after() throws Exception;

    @Before
    public void setUp() throws Exception {
        before();
    }

    @After
    public void wipeDown() throws Exception {
        after();
    }

    public ServiceArgs getTestServiceArgs() {
        ServiceArgs sa = new ServiceArgs();
        sa.setToken("Splunk " + SPLUNK_TOKEN);
        sa.setHost(SPLUNK_HOST);
        sa.setPort(Integer.parseInt(SPLUNK_PORT));
        sa.setScheme("http");
        return sa;
    }

    public void setSystemProperties() {
        System.setProperty(SPLUNK_ENABLED_KEY, "true");
        System.setProperty(SPLUNK_TOKEN_KEY, SPLUNK_TOKEN);
        System.setProperty(SPLUNK_HOST_KEY, SPLUNK_HOST);
        System.setProperty(SPLUNK_PORT_KEY, SPLUNK_PORT);
        System.setProperty(SPLUNK_HEC_URI_KEY, SPLUNK_HEC_URI);
    }

    public void clearSystemProperties() {
        System.clearProperty(SPLUNK_ENABLED_KEY);
        System.clearProperty(SPLUNK_TOKEN_KEY);
        System.clearProperty(SPLUNK_HOST_KEY);
        System.clearProperty(SPLUNK_PORT_KEY);
        System.clearProperty(SPLUNK_HEC_URI_KEY);
    }

    public String getURI() {
        return SPLUNK_HEC_URI;
    }
}
