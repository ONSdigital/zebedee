package com.github.onsdigital.zebedee.json;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Represents a user login session.
 * Created by david on 16/03/2015.
 */
public class Session {

    /**
     * The ID of this session.
     */
    public String id;

    /**
     * The user this session represents.
     */
    public String email;

    /**
     * The date-time at which the session started. This is useful for general information. Defaults to the current date.
     */
    public Date start = new Date();

    /**
     * The date-time at which the session was last accessed. This is useful for timeouts. Defaults to the current date.
     */
    public Date lastAccess = new Date();

    @Override
    public String toString() {
        return email + " (" + StringUtils.abbreviate(id, 8) + ")";
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null &&
                Session.class.isAssignableFrom(obj.getClass()) &&
                StringUtils.equals(id, ((Session) obj).id);
    }
}
