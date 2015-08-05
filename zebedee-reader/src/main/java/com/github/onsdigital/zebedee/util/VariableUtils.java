package com.github.onsdigital.zebedee.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by bren on 30/07/15.
 */
public class VariableUtils {

    /**
     * Reads varialbe from system properties, if not found in system properties reads from environment variables
     * @param name
     * @return
     */
    public static String getVariableValue(String name) {
        return StringUtils.defaultIfBlank(getSystemProperty(name), getEnv(name));
    }


    public static String getEnv(String name) {
        return System.getenv(name);
    }


    /**
     * Reads system property
     *
     * @param key
     * @return
     */
    public static String getSystemProperty(String key) {
        return System.getProperty(key);
    }
    
    
    public static void setProperty(String key, String value) {
        System.setProperty(key, value);
    }

}
