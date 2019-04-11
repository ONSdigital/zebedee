package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

public abstract class TestUtils {

    static final String ZEBEDEE_ROOT_TEST = "target/test-classes/test-content/";

    public static void initReaderConfig() {
        ReaderConfiguration.init(ZEBEDEE_ROOT_TEST);
    }

    public static void clearReaderConfig() {
        ReaderConfiguration.clear();
    }
}
