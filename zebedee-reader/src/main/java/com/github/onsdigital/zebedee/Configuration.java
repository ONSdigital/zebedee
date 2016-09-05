package com.github.onsdigital.zebedee;

import com.google.common.collect.ImmutableMap;
import com.splunk.Args;
import com.splunk.ServiceArgs;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Created by dave on 8/8/16.
 */
public class Configuration {

    /**
     * Provides methods for accessing Splunk configuration.
     */
    public static class SplunkConfiguration {

        public static final String CONFIG_MISSING_MSG = "Splunk Metrics reporting is enabled but configuration " +
                "parameter {0} was not found.";

        private static final String SPLUNK_ENABLED_ENV = "enable_splunk_reporting";
        private static final String SPLUNK_HEC_AUTH_TOKEN_ENV = "splunk_http_event_collection_auth_token";
        private static final String SPLUNK_HEC_HOST_ENV = "splunk_http_event_collection_host";
        private static final String SPLUNK_HEC_PORT_ENV = "splunk_http_event_collection_port";
        private static final String SPLUNK_HEC_URI_ENV = "splunk_http_event_collection_uri";

        private SplunkConfiguration() { /** static methods only */}

        public static boolean isSplunkEnabled() {
            // TODO DOES THIS FIX IT? CAN I GO HOME NOW?
            try {
                return BooleanUtils.toBoolean(getValue(SPLUNK_ENABLED_ENV));
            } catch (Exception ex) {
                return false;
            }
        }

        /**
         * @return {@link ServiceArgs} used when connecting to Splunk.
         */
        public static Args getServiceArgs() {
            return ServiceArgs.create(new ImmutableMap.Builder<String, Object>()
                    .put("token", "Splunk " + getValue(SPLUNK_HEC_AUTH_TOKEN_ENV))
                    .put("host", getValue(SPLUNK_HEC_HOST_ENV))
                    .put("port", Integer.parseInt(getValue(SPLUNK_HEC_PORT_ENV)))
                    .put("scheme", "http")
                    .build());
        }

        public static String getEventsCollectionURI() {
            return getValue(SPLUNK_HEC_URI_ENV);
        }

        static String getValue(String key) {
            String result = defaultIfBlank(getProperty(key), getenv(key));

            if (StringUtils.isEmpty(result)) {
                throw new RuntimeException(format(CONFIG_MISSING_MSG, key));
            }
            return result;
        }
    }
}
