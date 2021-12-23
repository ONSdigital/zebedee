package com.github.onsdigital.zebedee.content.page.home;

import com.google.gson.annotations.SerializedName;

public enum EmergencyBannerType {
    @SerializedName("notable_death")
    NOTABLE_DEATH,
    @SerializedName("national_emergency")
    NATIONAL_EMERGENCY,
    @SerializedName("local_emergency")
    LOCAL_EMERGENCY;
}
