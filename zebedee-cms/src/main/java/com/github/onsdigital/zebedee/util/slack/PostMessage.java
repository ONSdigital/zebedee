package com.github.onsdigital.zebedee.util.slack;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class PostMessage {
    public enum Emoji {
        HEAVY_EXCLAMATION_MARK(":heavy_exclamation_mark:"),
        CHART_WITH_UPWARDS_TREND(":chart_with_upwards_trend:");

        private String emoji;

        Emoji(String emoji) {
            this.emoji = emoji;
        }

        public String getEmoji() {
            return this.emoji;
        }
    }

    private String channel;
    private String username;
    @SerializedName("icon_emoji")
    private String emoji;
    private String text;
    private List<PostMessageAttachment> attachments;
    private String ts;

    public PostMessage() {
        this("", "", Emoji.CHART_WITH_UPWARDS_TREND, "");
    }

    public PostMessage(String username, String channel, Emoji emoji) {
        this(username, channel, emoji, "");
    }

    public PostMessage(String username, String channel, Emoji emoji, String text) {
        this.username = username;
        this.channel = channel;
        this.text = text;

        if(emoji == null) emoji = Emoji.CHART_WITH_UPWARDS_TREND;
        this.emoji = emoji.getEmoji();

        this.attachments = new ArrayList<>();
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public void setEmoji(Emoji emoji) {
        this.emoji = emoji.getEmoji();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<PostMessageAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<PostMessageAttachment> attachments) {
        this.attachments = attachments;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }
}