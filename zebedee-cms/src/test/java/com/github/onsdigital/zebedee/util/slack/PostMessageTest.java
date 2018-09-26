package com.github.onsdigital.zebedee.util.slack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PostMessageTest {

    @Test
    public void postMessageAttachmentsNotNull() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend);
        assertNotNull(pm);
        assertNotNull(pm.attachments);

        pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend, "");
        assertNotNull(pm);
        assertNotNull(pm.attachments);

        pm = new PostMessage();
        assertNotNull(pm);
        assertNotNull(pm.attachments);
    }

    @Test
    public void postMessageEmojiMapping() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend);
        assertNotNull(pm);
        assertEquals(pm.icon_emoji, ":chart_with_upwards_trend:");

        pm = new PostMessage("username", "channel", PostMessage.Emoji.HeavyExclamationMark);
        assertNotNull(pm);
        assertEquals(pm.icon_emoji, ":heavy_exclamation_mark:");

        pm = new PostMessage("username", "channel", null);
        assertNotNull(pm);
        assertEquals(pm.icon_emoji, ":chart_with_upwards_trend:");
    }

    @Test
    public void postMessageStoresUsername() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend);
        assertNotNull(pm);
        assertEquals(pm.username, "username");

        pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend, "text");
        assertNotNull(pm);
        assertEquals(pm.username, "username");
    }

    @Test
    public void postMessageStoresChannel() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend);
        assertNotNull(pm);
        assertEquals(pm.channel, "channel");

        pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend, "text");
        assertNotNull(pm);
        assertEquals(pm.channel, "channel");
    }

    @Test
    public void postMessageStoresText() {
        PostMessage pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend);
        assertNotNull(pm);
        assertEquals(pm.text, "");

        pm = new PostMessage("username", "channel", PostMessage.Emoji.ChartWithUpwardsTrend, "text");
        assertNotNull(pm);
        assertEquals(pm.text, "text");
    }

}
