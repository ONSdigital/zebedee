package com.github.onsdigital.zebedee.permissions.cmd;

import com.google.gson.annotations.SerializedName;

/**
 * @deprecated in favour of the dp-permissions-api. Once all dataset related APIs have been updated to use the
 *             dp-authorisation v2 library and JWT sessions are in use, this service will be removed.
 */
@Deprecated
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