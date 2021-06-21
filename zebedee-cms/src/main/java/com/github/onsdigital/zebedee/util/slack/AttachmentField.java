package com.github.onsdigital.zebedee.util.slack;

public class AttachmentField {
    private String title;
    private String message;
    private boolean isShort;

    public AttachmentField(String title, String message, boolean isShort) {
        this.title = title;
        this.message = message;
        this.isShort = isShort;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isShort() {
        return isShort;
    }
}
