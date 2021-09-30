package com.github.onsdigital.zebedee.model.publishing;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PublisherTest {

    private final String TEST_DIR_NAME = "src/test/resources/bpTestFolder";
    private Publisher publisher;

    @Before
    public void setup() throws IOException {
        this.publisher = new Publisher();
    }

    @Test
    public void testGetUrisConverted() {

        //Given {a list of uris ready to be sent to kafka}
        List<String> testUris = new ArrayList<>();
        testUris.add("/testUris0/data.json");
        testUris.add("/testUri1");
        testUris.add("/testUris2/data.json");
        testUris.add("/testUris3");

        //When {sending a list of uris to kafka}
        List<String> actuals = publisher.getUrlsConverted(testUris);

        //Then {size of the list unchanged}.
        assertEquals(actuals.size(), testUris.size());
        //And {none of the uris have "data.json" string}
        assertTrue(!actuals.contains("/testUris0/data.json"));
        assertTrue(actuals.contains("/testUris0"));
        assertTrue(actuals.contains("/testUris2"));
    }
}