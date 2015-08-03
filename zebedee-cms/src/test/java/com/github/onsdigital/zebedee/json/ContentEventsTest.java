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
        Events events = new Events();

        // when
        // we try and get the most recent event for a type.
        Event result = events.mostRecentEventForType(EventType.COMPLETED);

        // then
        // the result is null.
        assertNull(result);
    }

    @Test
    public void shouldReturnMostRecentEvent() {

        // given
        // a number of content events.
        Events events = new Events();
        events.add(new Event(new DateTime(2015, 1, 1, 0, 0).toDate(), EventType.COMPLETED, "user@test.com"));
        Event mostRecentEvent = new Event(new DateTime(2015, 1, 3, 0, 0).toDate(), EventType.COMPLETED, "user@test.com");
        events.add(mostRecentEvent);
        events.add(new Event(new DateTime(2015, 1, 2, 0, 0).toDate(), EventType.COMPLETED, "user@test.com"));

        // when
        // we try and get the most recent event for a type.
        Event result = events.mostRecentEventForType(EventType.COMPLETED);

        // then
        // the result is the most recent event.
        assertEquals(mostRecentEvent, result);
    }

    @Test
    public void mostRecentEventShouldReturnMostRecentEvent() {

        // given
        // a number of content events.
        Events events = new Events();
        events.add(new Event(new DateTime(2015, 1, 1, 0, 0).toDate(), EventType.COMPLETED, "user@test.com"));
        Event mostRecentEvent = new Event(new DateTime(2015, 1, 3, 0, 0).toDate(), EventType.COMPLETED, "user@test.com");
        events.add(mostRecentEvent);
        events.add(new Event(new DateTime(2015, 1, 2, 0, 0).toDate(), EventType.COMPLETED, "user@test.com"));

        // when
        // we try and get the most recent event for a type.
        Event result = events.mostRecentEvent();

        // then
        // the result is the most recent event.
        assertEquals(mostRecentEvent, result);
    }
}
