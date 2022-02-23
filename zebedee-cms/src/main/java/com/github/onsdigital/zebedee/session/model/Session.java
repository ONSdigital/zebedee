package com.github.onsdigital.zebedee.session.model;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link Session} Is an immutable, thread safe representation of a user's session. This class is the way in which the
 * current user's identity should be passed throughout the application so as to maintain a consistent requirement and
 * make propagation of a user's session simpler.
 */
@ThreadSafe
@Immutable
public class Session {

    /**
     * The ID of this session.
     */
    private final String id;

    /**
     * The user this session represents.
     */
    private final String email;

    /**
     * The list of groups the user is a member of.
     */
    private final List<String> groups;

    /**
     * Construct a new Session from the details provided.
     *
     * @param id     the unqiue ID of the session.
     * @param email  the user email the session belongs to.
     * @param groups the list of ID for the groups the user is a member of
     */
    public Session(String id, String email, List<String> groups) {
        this.id = id;
        this.email = email;

        if (groups == null) {
            groups = new ArrayList<>();
        }
        this.groups = Collections.unmodifiableList(new ArrayList<>(groups));
    }

    /**
     * Construct a new Session without any group membership.
     *
     * @param id     the unqiue ID of the session.
     * @param email  the user email the session belongs to.
     */
    public Session(String id, String email) {
        this(id, email, new ArrayList<>());
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getGroups() {
        return groups;
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
                this.getClass() == obj.getClass() &&
                StringUtils.equals(id, ((Session) obj).id);
    }
}
