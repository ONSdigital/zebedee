package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides functionality for getting properties values.
 */
public class PropertiesUtil {

    private static final String LANG_RESOURCE_PATH = "/collection_event_history_en.properties";
    private static Properties languageProperties = null;

    public static String getProperty(String key) throws ZebedeeException {
        if (languageProperties == null) {
            try (InputStream inputStream = PropertiesUtil.class.getResourceAsStream(LANG_RESOURCE_PATH)) {
                languageProperties = new Properties();
                languageProperties.load(inputStream);
            } catch (IOException io) {
                throw new UnexpectedErrorException(io.getMessage(), 500);
            }
        }
        String result = (String)languageProperties.get(key);
        return StringUtils.isNotEmpty(result) ? result : key;
    }
}
