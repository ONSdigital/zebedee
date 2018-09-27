package com.github.onsdigital.zebedee.util.slack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PostMessageTest {

    @Test
    public void postMessageAttachmentsNotNull() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND);
        assertNotNull(pm);
        assertNotNull(pm.getAttachments());

        pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND, "");
        assertNotNull(pm);
        assertNotNull(pm.getAttachments());

        pm = new PostMessage();
        assertNotNull(pm);
        assertNotNull(pm.getAttachments());
    }

    @Test
    public void postMessageEmojiMapping() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND);
        assertNotNull(pm);
        assertEquals(pm.getEmoji(), ":chart_with_upwards_trend:");

        pm = new PostMessage("username", "channel", PostMessage.Emoji.HEAVY_EXCLAMATION_MARK);
        assertNotNull(pm);
        assertEquals(pm.getEmoji(), ":heavy_exclamation_mark:");

        pm = new PostMessage("username", "channel", null);
        assertNotNull(pm);
        assertEquals(pm.getEmoji(), ":chart_with_upwards_trend:");
    }

    @Test
    public void postMessageStoresUsername() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND);
        assertNotNull(pm);
        assertEquals(pm.getUsername(), "username");

        pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND, "text");
        assertNotNull(pm);
        assertEquals(pm.getUsername(), "username");
    }

    @Test
    public void postMessageStoresChannel() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND);
        assertNotNull(pm);
        assertEquals(pm.getChannel(), "channel");

        pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND, "text");
        assertNotNull(pm);
        assertEquals(pm.getChannel(), "channel");
    }

    @Test
    public void postMessageStoresText() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND);
        assertNotNull(pm);
        assertEquals(pm.getText(), "");

        pm = new PostMessage("username", "channel", PostMessage.Emoji.CHART_WITH_UPWARDS_TREND, "text");
        assertNotNull(pm);
        assertEquals(pm.getText(), "text");
    }

}
