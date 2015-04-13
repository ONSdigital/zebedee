package com.github.onsdigital.zebedee.json;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ContentEventsTest {

    @Test
    public void shouldReturnNullIfMostRecentDoesNotExist() {

        // given
        // an empty collection of content events.
        ContentEvents events = new ContentEvents();

        // when
        // we try and get the most recent event for a type.
        ContentEvent result = events.mostRecentEventForType(ContentEventType.COMPLETED);

        // then
        // the result is null.
        assertNull(result);
    }

    @Test
    public void shouldReturnMostRecentEvent() {

        // given
        // a number of content events.
        ContentEvents events = new ContentEvents();
        events.add(new ContentEvent(new DateTime(2015, 1, 1, 0, 0).toDate(), ContentEventType.COMPLETED, "user@test.com"));
        ContentEvent mostRecentEvent = new ContentEvent(new DateTime(2015, 1, 3, 0, 0).toDate(), ContentEventType.COMPLETED, "user@test.com");
        events.add(mostRecentEvent);
        events.add(new ContentEvent(new DateTime(2015, 1, 2, 0, 0).toDate(), ContentEventType.COMPLETED, "user@test.com"));

        // when
        // we try and get the most recent event for a type.
        ContentEvent result = events.mostRecentEventForType(ContentEventType.COMPLETED);

        // then
        // the result is the most recent event.
        assertEquals(mostRecentEvent, result);
    }
}
