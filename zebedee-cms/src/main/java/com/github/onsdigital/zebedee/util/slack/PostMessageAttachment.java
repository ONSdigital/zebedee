package com.github.onsdigital.zebedee.util.slack;

import java.util.ArrayList;
import java.util.List;

public class PostMessageAttachment {
    public enum Color {
        GOOD("good"),
        WARNING("warning"),
        DANGER("danger");

        private String color;

        Color(String color) {
            this.color = color;
        }

        public String getColor() {
            return this.color;
        }
    }

    private String pretext;
    private String text;
    private String title;
    private String color;
    private List<PostMessageField> fields;

    public PostMessageAttachment() {
        this("", "", Color.GOOD);
    }

    public PostMessageAttachment(String text, String title, Color color) {
        this.fields = new ArrayList<>();
        this.text = text;
        this.title = title;
        this.color = color.getColor();
    }

    public String getPretext() {
        return pretext;
    }

    public void setPretext(String pretext) {
        this.pretext = pretext;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<PostMessageField> getFields() {
        return fields;
    }

    public void setFields(List<PostMessageField> fields) {
        this.fields = fields;
    }
}
