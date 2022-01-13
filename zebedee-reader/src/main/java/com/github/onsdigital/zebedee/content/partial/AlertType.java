package com.github.onsdigital.zebedee.content.partial;

import com.google.gson.annotations.SerializedName;

public enum AlertType {
    @SerializedName("alert")
    ALERT,
    @SerializedName("correction")
    CORRECTION
}
