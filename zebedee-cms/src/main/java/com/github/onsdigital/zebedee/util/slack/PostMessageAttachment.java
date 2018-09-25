package com.github.onsdigital.zebedee.util.slack;

import java.util.ArrayList;
import java.util.List;

public class PostMessageAttachment {
    public enum Color {
        Good,
        Warning,
        Danger
    }

    public String pretext;
    public String text;
    public String title;
    public String color;
    public List<PostMessageField> fields;

    public PostMessageAttachment() {
        this.fields = new ArrayList<>();
    }

    public PostMessageAttachment(String text, String title, Color color) {
        this();
        this.text = text;
        this.title = title;
        this.fields = new ArrayList<>();
        switch(color) {
            case Good:
                this.color = "good";
            case Danger:
                this.color = "danger";
            case Warning:
                this.color = "warning";
            default:
                this.color = "danger";
        }
    }
}
