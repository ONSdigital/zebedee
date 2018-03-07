package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.session.model.Session;

import static java.util.Objects.requireNonNull;

/**
 * Created by dave on 07/03/2018.
 */
public class UserIdentity implements JSONable {

    private String email;
    private String token;

    public UserIdentity(Session session) {
        requireNonNull(session);
        this.email = session.getEmail();
        this.token = session.getId();
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toJSON() {
        return ContentUtil.serialise(this);
    }
}
