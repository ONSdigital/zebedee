package com.github.onsdigital.zebedee.permissions.cmd;

import com.google.gson.annotations.SerializedName;

public enum PermissionType {

    @SerializedName("CREATE")
    CREATE,

    @SerializedName("READ")
    READ,

    @SerializedName("UPDATE")
    UPDATE,

    @SerializedName("DELETE")
    DELETE,
}