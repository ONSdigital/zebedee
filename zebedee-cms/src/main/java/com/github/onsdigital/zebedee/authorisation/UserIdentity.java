package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.json.JSONable;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static java.util.Objects.requireNonNull;

public class UserIdentity implements JSONable {

    private String identifier;

    public UserIdentity(Session session) {
        requireNonNull(session);
        this.identifier = session.getEmail();
    }

    public UserIdentity(String email) {
        requireNonNull(email);
        this.identifier = email;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toJSON() {
        return ContentUtil.serialise(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UserIdentity identity = (UserIdentity) o;

        return new EqualsBuilder()
                .append(getIdentifier(), identity.getIdentifier())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getIdentifier())
                .toHashCode();
    }
}
