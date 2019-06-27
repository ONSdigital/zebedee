package com.github.onsdigital.zebedee.authorisation;

import com.google.gson.annotations.SerializedName;

public enum DatasetPermissionType {

    @SerializedName("CREATE")
    CREATE,

    @SerializedName("READ")
    READ,

    @SerializedName("UPDATE")
    UPDATE,

    @SerializedName("DELETE")
    DELETE,
}