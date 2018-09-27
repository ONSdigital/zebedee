package com.github.onsdigital.zebedee.util.slack;

import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class PostMessageAttachmentTest {

    @Test
    public void postMessageAttachmentFieldsNotNull() {
        PostMessageAttachment pma = new PostMessageAttachment();
        assertNotNull(pma);
        assertNotNull(pma.getFields());
    }

    @Test
    public void postMessageAttachmentStoresTextAndTitle() {
        PostMessageAttachment pma = new PostMessageAttachment("text", "title", PostMessageAttachment.Color.DANGER);
        assertNotNull(pma);
        assertNotNull(pma.getFields());
        assertEquals(pma.getTitle(), "title");
        assertEquals(pma.getText(), "text");
    }

    @Test
    public void postMessageAttachmentColorMapping() {
        PostMessageAttachment pma = new PostMessageAttachment("text", "title", PostMessageAttachment.Color.DANGER);
        assertNotNull(pma);
        assertEquals(pma.getColor(), "danger");

        pma = new PostMessageAttachment("text", "title", PostMessageAttachment.Color.GOOD);
        assertNotNull(pma);
        assertEquals(pma.getColor(), "good");

        pma = new PostMessageAttachment("text", "title", PostMessageAttachment.Color.WARNING);
        assertNotNull(pma);
        assertEquals(pma.getColor(), "warning");
    }

}
