package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.session.model.Session;

import static java.util.Objects.requireNonNull;

/**
 * Created by dave on 07/03/2018.
 */
public class UserIdentity implements JSONable {

    private String email;
    private String token;
    private boolean isAdmin;
    private boolean isEditor;

    public UserIdentity(Session session, PermissionDefinition permissions) {
        requireNonNull(session);
        this.email = session.getEmail();
        this.token = session.getId();
        this.isAdmin = permissions.admin;
        this.isEditor = permissions.editor;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isEditor() {
        return isEditor;
    }

    @Override
    public String toJSON() {
        return ContentUtil.serialise(this);
    }
}
