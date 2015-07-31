package com.github.onsdigital.zebedee.reader.configuration;

/**
 * Created by bren on 31/07/15.
 */
public class TestConfiguration {

    private static String TEST_ZEBEDEE_ROOT = "target/zebedee";

    public static String getTestZebedeeRoot() {
        return TEST_ZEBEDEE_ROOT;
    }

    public static void initializeTestConfiguration() {
        ReaderConfiguration.setDefatultZebedeeRoot(TEST_ZEBEDEE_ROOT);
    }
}
