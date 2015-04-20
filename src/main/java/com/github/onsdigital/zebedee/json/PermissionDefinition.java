package com.github.onsdigital.zebedee.json;

import java.util.Set;

/**
 * Provides a description of permissions for a user.
 */
public class PermissionDefinition {

    public String email;
    public boolean admin;
    public boolean editor;
    public Set<String> contentOwnerPaths;
}
