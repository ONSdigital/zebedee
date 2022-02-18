package com.github.onsdigital.zebedee.session.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * The legacy session model used by the filesystem implementation of the sessions store
 *
 * @deprecated in favour of {@link Session}. This legacy session is only used by the old files on disk sessions store
 *             which will soon be removed in favour of the new JWT based sessions.
 */
@Deprecated
public class LegacySession {

    /**
     * The ID of this session.
     */
    private final String id;

    /**
     * The user this session represents.
     */
    private final String email;

    /**
     * The date-time at which the session was last accessed. This is useful for timeouts.
     */
    private Date lastAccess;

    /**
     * Construct a new LegacySession from the details provided.
     *
     * @param id         the unqiue ID of the session.
     * @param email      the user email the session belongs to.
     * @param lastAccess the time the session was last accessed
     */
    public LegacySession(final String id, final String email) {
        this.id = id;
        this.email = email;
        this.lastAccess = new Date();
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Date getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(Date lastAccess) {
        this.lastAccess = lastAccess;
    }

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
                StringUtils.equals(id, ((LegacySession) obj).id);
    }
}
