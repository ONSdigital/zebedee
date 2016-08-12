package com.github.onsdigital.zebedee;

import com.google.common.collect.ImmutableMap;
import com.splunk.Args;
import com.splunk.ServiceArgs;
import org.apache.commons.lang3.BooleanUtils;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Created by dave on 8/8/16.
 */
public class Configuration {

    public static class SplunkConfiguration {

        private static final String SPLUNK_ENABLED_ENV = "enable_splunk_reporting";
        private static final String SPLUNK_HEC_AUTH_TOKEN_ENV = "splunk_http_event_collection_auth_token";
        private static final String SPLUNK_HEC_HOST_ENV = "splunk_http_event_collection_host";
        private static final String SPLUNK_HEC_PORT_ENV = "splunk_http_event_collection_port";
        private static final String SPLUNK_HEC_URI_ENV = "splunk_http_event_collection_uri";

        private SplunkConfiguration() { /** static methods only */ }

        public static boolean isSplunkEnabled() {
            return BooleanUtils.toBoolean(getValue(SPLUNK_ENABLED_ENV));
        }

        public static Args getServiceArgs() {
            return ServiceArgs.create(new ImmutableMap.Builder<String, Object>()
                    .put("token", "Splunk " + defaultIfBlank(getValue(SPLUNK_HEC_AUTH_TOKEN_ENV), ""))
                    .put("host", defaultIfBlank(getValue(SPLUNK_HEC_HOST_ENV), ""))
                    .put("port", Integer.parseInt(defaultIfBlank(getValue(SPLUNK_HEC_PORT_ENV), "0")))
                    .put("scheme", "http")
                    .build());
        }

        public static String getEventsCollectionURI() {
            // todo what if this is missing?
            return defaultIfBlank(getValue(SPLUNK_HEC_URI_ENV), "");
        }

    }

    static String getValue(String key) {
        return defaultIfBlank(getProperty(key), getenv(key));
    }
}
