package com.github.onsdigital.zebedee.model.publishing;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class PublisherTest {

    private final String TEST_DIR_NAME = "src/test/resources/bpTestFolder";
    private Publisher publisher;

    @Before
    public void setup() throws IOException {
        this.publisher = new Publisher();
    }

    @Test
    public void testConvertUriWithJsonForEvent() {

        //Given {a single uri with data.json ready to be sent to kafka}
        String testUri = "/testUri0/data.json";

        //When {sending a uris to kafka}
        String actuals = publisher.convertUriForEvent(testUri);

        System.out.println(actuals);

        //Then {uri does not have "data.json" string}
        assertTrue(!actuals.contains("/testUri0/data.json"));
        assertTrue(actuals.contains("/testUri0"));
    }

    @Test
    public void testConvertUriWithOutJsonForEvent() {

        //Given {a single uri without data.json ready to be sent to kafka}
        String testUri1 = "/testUri1";

        //When {sending a uri to kafka}
        String actuals = publisher.convertUriForEvent(testUri1);

        System.out.println(actuals);

        //Then {uris returns the original string}
        assertTrue(actuals.contains("/testUri1"));
    }
}