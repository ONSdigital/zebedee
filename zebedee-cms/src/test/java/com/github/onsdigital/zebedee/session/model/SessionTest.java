package com.github.onsdigital.zebedee.session.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SessionTest {

    private final static String SESSION_TOKEN = "7be8cdc8f0b63603eb34490c2fcb91a0a2d01a9c292dd8baf397779a22d917d9";
    private final static String EMAIL = "someone@example.com";

    @Test
    public void constructor_shouldBeImmutable() throws Exception {
        List<String> originalGroups = new ArrayList<>();
        originalGroups.add("group1");
        originalGroups.add("group2");
        String newGroup = "group3";

        Session session = new Session(SESSION_TOKEN, EMAIL, originalGroups);

        originalGroups.add(newGroup);

        assertEquals(2, session.getGroups().size());
        assertFalse(session.getGroups().contains(newGroup));
    }

    @Test
    public void getGroups_shouldBeImmutable() throws Exception {
        List<String> originalGroups = new ArrayList<>();
        originalGroups.add("group1");
        originalGroups.add("group2");
        String newGroup = "group3";
        Session session = new Session(SESSION_TOKEN, EMAIL, originalGroups);

        List<String> returnedGroups = session.getGroups();

        assertThrows(UnsupportedOperationException.class, () -> returnedGroups.add(newGroup));
    }
}
