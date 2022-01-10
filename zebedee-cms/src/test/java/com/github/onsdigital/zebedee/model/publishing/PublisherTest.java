package com.github.onsdigital.zebedee.model.publishing;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublisherTest {

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
        String actual = publisher.convertUriForEvent(testUri);

        System.out.println(actual);

        //Then {uri does not have "data.json" string}
        assertFalse(actual.contains("/testUri0/data.json"));
        assertTrue(actual.contains("/testUri0"));
    }

    @Test
    public void testConvertUriWithOutJsonForEvent() {

        //Given {a single uri without data.json ready to be sent to kafka}
        String testUri1 = "/testUri1";

        //When {sending a uri to kafka}
        String actual = publisher.convertUriForEvent(testUri1);

        //Then {uris returns the original string}
        assertTrue(actual.contains("/testUri1"));
    }

    @Test
    public void testisValidCMDDatasetURISuccess() {

        //Given {A valid uri is passed}
        String testUri = "/datasets/cpih01/editions/timeseries/versions/version";

        //When {Check for uri validity}
        boolean actual = publisher.isValidCMDDatasetURI(testUri);

        //Then {The uri is valid}
        assertTrue(actual);
    }

    @Test
    public void testisValidCMDDatasetURIFailure() {

        //Given {An invalid uri with hypen is passed}
        String testUri = "/dataset/cpih/editions/timeseries/";

        //When {Check for uri validity}
        boolean actual = publisher.isValidCMDDatasetURI(testUri);

        //Then {The uri is not valid}
        assertFalse(actual);
    }

    @Test
    public void testisValidCMDDatasetURISuccessWithHyphen() {

        //Given {A valid uri is passed}
        String testUri = "/datasets/cpih01-test-7/editions/time-series/versions/8";

        //When {Check for uri validity}
        boolean actual = publisher.isValidCMDDatasetURI(testUri);

        //Then {The uri is valid}
        assertTrue(actual);
    }

}