package com.github.onsdigital.zebedee.util.slack;

import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class PostMessageAttachmentTest {

    @Test
    public void postMessageAttachmentFieldsNotNull() {
        PostMessageAttachment pma = new PostMessageAttachment();
        assertNotNull(pma);
        assertNotNull(pma.fields);
    }

    @Test
    public void postMessageAttachmentStoresTextAndTitle() {
        PostMessageAttachment pma = new PostMessageAttachment("text", "title", PostMessageAttachment.Color.Danger);
        assertNotNull(pma);
        assertNotNull(pma.fields);
        assertEquals(pma.title, "title");
        assertEquals(pma.text, "text");
    }

    @Test
    public void postMessageAttachmentColorMapping() {
        PostMessageAttachment pma = new PostMessageAttachment("text", "title", PostMessageAttachment.Color.Danger);
        assertNotNull(pma);
        assertEquals(pma.color, "danger");

        pma = new PostMessageAttachment("text", "title", PostMessageAttachment.Color.Good);
        assertNotNull(pma);
        assertEquals(pma.color, "good");

        pma = new PostMessageAttachment("text", "title", PostMessageAttachment.Color.Warning);
        assertNotNull(pma);
        assertEquals(pma.color, "warning");
    }

}
