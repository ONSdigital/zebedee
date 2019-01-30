package com.github.onsdigital.zebedee.persistence;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CollectionEventTypeTest {

    @Test
    public void shouldGetExpectedEventTypeByID() {
        List<CollectionEventType> events = Arrays.asList(CollectionEventType.values());
        Collections.shuffle(events);
        events.stream()
                .forEach(e -> {
                    System.out.println("getEventByID: " + e.name() + " " + e.getId());
                    assertThat("incorrect CollectionEventType for ID",
                            CollectionEventType.getById(e.getId()), equalTo(e));
                });
    }

    @Test
    public void shouldReturnUnspecifiedForInvalidID() {
        assertThat("expected CollectionEventType.UNSPECIFIED"
                , CollectionEventType.getById(-1), equalTo(CollectionEventType.UNSPECIFIED));
    }
}
