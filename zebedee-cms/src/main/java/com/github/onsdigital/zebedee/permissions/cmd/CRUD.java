package com.github.onsdigital.zebedee.permissions.cmd;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;


public class CRUD {

    private Set<PermissionType> permissions;

    public CRUD() {
        this.permissions = new HashSet<>();
    }

    public CRUD(PermissionType... grantedPermissions) {
        this.permissions = new LinkedHashSet<>();
        permit(grantedPermissions);
    }

    public Set<PermissionType> getPermissions() {
        return permissions;
    }

    public CRUD permit(PermissionType... permissions) {
        for (PermissionType p : permissions) {
            this.permissions.add(p);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CRUD that = (CRUD) o;
        return this.permissions.equals(that.permissions);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getPermissions())
                .toHashCode();
    }

    public static CRUD permitCreateReadUpdateDelete() {
        return new CRUD().permit(CREATE, READ, UPDATE, DELETE);
    }

    public static CRUD permitRead() {
        return new CRUD().permit(READ);
    }

    public static CRUD permitNone() {
        return new CRUD();
    }
}
