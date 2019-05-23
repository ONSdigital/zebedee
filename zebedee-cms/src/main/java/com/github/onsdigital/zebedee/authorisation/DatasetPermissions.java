package com.github.onsdigital.zebedee.authorisation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class DatasetPermissions {

    private Set<DatasetPermissionType> permissions;

    public DatasetPermissions() {
        this.permissions = new HashSet<>();
    }

    public DatasetPermissions(DatasetPermissionType... grantedPermissions) {
        this.permissions = new LinkedHashSet<>();
        permit(grantedPermissions);
    }

    public Set<DatasetPermissionType> getPermissions() {
        return permissions;
    }

    public DatasetPermissions setPermissions(Set<DatasetPermissionType> permissions) {
        this.permissions = permissions;
        return this;
    }

    public DatasetPermissions grantPermission(DatasetPermissionType permission) {
        this.permissions.add(permission);
        return this;
    }

    public DatasetPermissions permit(DatasetPermissionType... permissions) {
        for (DatasetPermissionType p : permissions) {
            this.permissions.add(p);
        }
        return this;
    }
}
