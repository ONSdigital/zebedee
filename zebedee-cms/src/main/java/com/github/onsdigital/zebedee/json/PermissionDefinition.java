package com.github.onsdigital.zebedee.json;

/**
 * Provides a description of permissions for a user.
 */
public class PermissionDefinition {

    private String email;
    private Boolean admin;
    private Boolean editor;

    public String getEmail() {
        return email;
    }

    public PermissionDefinition setEmail(String email) {
        this.email = email;
        return this;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public PermissionDefinition isAdmin(Boolean admin) {
        this.admin = admin;
        return this;
    }

    public Boolean isEditor() {
        return editor;
    }

    public PermissionDefinition isEditor(Boolean editor) {
        this.editor = editor;
        return this;
    }
}
