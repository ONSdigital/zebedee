package com.github.onsdigital.zebedee.model.publishing;

import org.junit.Assert;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class PostPublisherTest {

    @Test
    public void isIndexedUriShouldBeTrueIfNotVersioned() {

        // Given a URI that is not that of a versioned file
        String uri = "/some/unversioned/uri";

        // When the isIndexedUri method is called
        boolean isIndexed = PostPublisher.isIndexedUri(uri);

        // Then the result should be true, ie, it should be indexed.
        assertTrue(isIndexed);
    }

    @Test
    public void isIndexedUriShouldBeFalseIfVersioned() {

        // Given a URI that is that of a versioned file
        String uri = "/some/versioned/uri/previous/v1";

        // When the isIndexedUri method is called
        boolean isIndexed = PostPublisher.isIndexedUri(uri);

        // Then the result should be false, ie, it should not be indexed.
        assertFalse(isIndexed);
    }

    @Test
    public void testConvertUriWithJsonForEvent() {

        //Given {a single uri with data.json ready to be sent to kafka}
        String testUri = "/testUri0/data.json";

        //When {sending a uris to kafka}
        String actual = PostPublisher.convertUriForEvent(testUri);

        System.out.println(actual);

        //Then {uri does not have "data.json" string}
        assertFalse(actual.contains("/testUri0/data.json"));
        Assert.assertTrue(actual.contains("/testUri0"));
    }

    @Test
    public void testConvertUriWithOutJsonForEvent() {

        //Given {a single uri without data.json ready to be sent to kafka}
        String testUri1 = "/testUri1";

        //When {sending a uri to kafka}
        String actual = PostPublisher.convertUriForEvent(testUri1);

        //Then {uris returns the original string}
        Assert.assertTrue(actual.contains("/testUri1"));
    }
}
