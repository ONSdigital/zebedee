package com.github.onsdigital.zebedee.model;

import org.apache.commons.lang3.StringUtils;

public class Configuration {
    private static final String DEFAULT_BASE_URL = "http://localhost:8082";

    public static String getBaseUrl() {
        return StringUtils.defaultIfBlank(getValue("baseUrl"), DEFAULT_BASE_URL);
    }

    static String getValue(String key) {
        return StringUtils.defaultIfBlank(System.getProperty(key), System.getenv(key));
    }
}
