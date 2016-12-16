package com.github.onsdigital.zebedee.model.publishing;

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
}
