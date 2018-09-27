package com.github.onsdigital.zebedee.util.slack;

import com.google.gson.annotations.SerializedName;

public class PostMessageField {
    private String title;
    private String value;

    @SerializedName("short")
    private boolean isShort;

    public PostMessageField(String title, String value) {
        this(title, value, false);
    }

    public PostMessageField(String title, String value, boolean isShort) {
        this.title = title;
        this.value = value;
        this.isShort = isShort;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isShort() {
        return isShort;
    }

    public void setIsShort(boolean aShort) {
        isShort = aShort;
    }
}
