package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.json.JSONable;
import com.github.onsdigital.zebedee.session.model.Session;

import static java.util.Objects.requireNonNull;

public class UserIdentity implements JSONable {

    private String email;

    public UserIdentity(Session session) {
        requireNonNull(session);
        this.email = session.getEmail();
    }

    public UserIdentity(String email) {
        requireNonNull(email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toJSON() {
        return ContentUtil.serialise(this);
    }
}
