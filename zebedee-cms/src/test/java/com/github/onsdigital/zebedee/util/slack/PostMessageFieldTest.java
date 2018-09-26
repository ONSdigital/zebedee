package com.github.onsdigital.zebedee.util.slack;

import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class PostMessageFieldTest {

    @Test
    public void postMessageFieldStoresTitleValueAndIsShort() {
        PostMessageField pmf = new PostMessageField("title", "value");
        assertNotNull(pmf);
        assertEquals(pmf.value, "value");
        assertEquals(pmf.title, "title");
        assertEquals(pmf.isShort, false);

        pmf = new PostMessageField("title", "value", true);
        assertNotNull(pmf);
        assertEquals(pmf.value, "value");
        assertEquals(pmf.title, "title");
        assertEquals(pmf.isShort, true);
    }

}
