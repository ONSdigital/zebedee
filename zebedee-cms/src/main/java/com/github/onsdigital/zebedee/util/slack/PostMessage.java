package com.github.onsdigital.zebedee.util.slack;

import java.util.List;

public class PostMessage {
    public String channel;
    public String username;
    public String icon_emoji;
    public String text;
    public List<PostMessageAttachment> attachments;
    public String ts;
}