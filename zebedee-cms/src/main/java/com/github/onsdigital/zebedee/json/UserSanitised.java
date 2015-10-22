package com.github.onsdigital.zebedee.json;

import org.apache.commons.lang3.BooleanUtils;

/**
 * Represents a reduced view of user account, suitable for sending to clients via the API.
 * NB this record intentionally does not contain any authentication, encryption or permission-related information.
 * This is purely acconut information.
 */
public class UserSanitised {

    public String name;
    public String email;

    /**
     * This field is {@link Boolean} rather than <code>boolean</code> so that it can be <code>null</code> in an update message.
     * This ensures the value won't change unless explicitly specified.
     */
    public Boolean inactive;

    public Boolean temporaryPassword;
    public String lastAdmin;

    @Override
    public String toString() {
        return name + ", " + email + (BooleanUtils.isTrue(inactive) ? " (inactive)" : "");
    }
}
