package com.github.onsdigital.zebedee.util;

import com.google.gson.annotations.SerializedName;

public class PostMessageField {
    public String title;
    public String value;

    @SerializedName("short")
    public boolean isShort;

    public PostMessageField(String title, String value) {
        this.title = title;
        this.value = value;
    }

    public PostMessageField(String title, String value, boolean isShort) {
        this.title = title;
        this.value = value;
        this.isShort = isShort;
    }
}
