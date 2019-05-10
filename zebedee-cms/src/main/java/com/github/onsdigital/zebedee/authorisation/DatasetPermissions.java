package com.github.onsdigital.zebedee.authorisation;

import java.util.HashSet;
import java.util.Set;

public class DatasetPermissions {

    private Set<DatasetPermissionType> permissions;

    public DatasetPermissions() {
        this.permissions = new HashSet<>();
    }

    public DatasetPermissions(DatasetPermissionType... grantedPermissions) {
        this.permissions = new HashSet<>();
        for (DatasetPermissionType p : grantedPermissions) {
            this.permissions.add(p);
        }
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
}
