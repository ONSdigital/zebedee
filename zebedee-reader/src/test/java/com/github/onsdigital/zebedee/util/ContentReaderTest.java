package com.github.onsdigital.zebedee.util;

import org.junit.Before;

/**
 * Created by bren on 30/07/15.
 */
public class ContentReaderTest {

    private final static String TEST_CONTENT_DIR = "target/zebedee";
    private ContentReader contentReader;

    @Before
    private void createContentReader() {
        this.contentReader =  new ContentReader(TEST_CONTENT_DIR);
    }



}
