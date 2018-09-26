package com.github.onsdigital.zebedee.util.slack;

import java.util.ArrayList;
import java.util.List;

public class PostMessage {
    public enum Emoji {
        HeavyExclamationMark,
        ChartWithUpwardsTrend
    }

    public String channel;
    public String username;
    public String icon_emoji;
    public String text;
    public List<PostMessageAttachment> attachments;
    public String ts;

    public PostMessage() {
        this.attachments = new ArrayList<>();
    }

    public PostMessage(String username, String channel, Emoji emoji) {
        this(username, channel, emoji, "");
    }

    public PostMessage(String username, String channel, Emoji emoji, String text) {
        this();

        this.username = username;
        this.channel = channel;
        this.text = text;
        
        if(emoji == null) emoji = Emoji.ChartWithUpwardsTrend;
        switch(emoji) {
            case HeavyExclamationMark:
                this.icon_emoji = ":heavy_exclamation_mark:";
                break;
            case ChartWithUpwardsTrend:
                this.icon_emoji = ":chart_with_upwards_trend:";
                break;
            default:
                this.icon_emoji = ":chart_with_upwards_trend:";
                break;
        }
    }
}