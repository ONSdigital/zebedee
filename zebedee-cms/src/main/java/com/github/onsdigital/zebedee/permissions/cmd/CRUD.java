package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.servicetokens.model.ServiceAccount;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;

/**
 * @deprecated in favour of the dp-permissions-api. Once all dataset related APIs have been updated to use the
 *             dp-authorisation v2 library and JWT sessions are in use, this service will be removed.
 */
@Deprecated
public class CRUD {

    private Set<PermissionType> permissions;

    public CRUD() {
        this.permissions = new HashSet<>();
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

    public static CRUD grantServiceAccountDatasetCreateReadUpdateDelete(GetPermissionsRequest request,
                                                                        ServiceAccount serviceAccount) {
        info().serviceAccountID(serviceAccount)
                .datasetID(request.getDatasetID())
                .log("granting full CRUD permissions to service account");
        return new CRUD().permit(CREATE, READ, UPDATE, DELETE);
    }

    public static CRUD grantUserDatasetCreateReadUpdateDelete(GetPermissionsRequest request) {
        info().collectionID(request.getCollectionID())
                .datasetID(request.getDatasetID())
                .email(request.getSession())
                .log("granting full CRUD permissions to user");
        return new CRUD().permit(CREATE, READ, UPDATE, DELETE);
    }

    public static CRUD grantUserDatasetRead(GetPermissionsRequest request) {
        info().collectionID(request.getCollectionID())
                .datasetID(request.getDatasetID())
                .email(request.getSession())
                .log("granting READ permission to user");
        return new CRUD().permit(READ);
    }

    public static CRUD grantUserNone(GetPermissionsRequest request, String message) {
        info().collectionID(request.getCollectionID())
                .datasetID(request.getDatasetID())
                .email(request.getSession())
                .log(message);
        return new CRUD();
    }

    public static CRUD grantUserInstanceCreateReadUpdateDelete(GetPermissionsRequest request) {
        info().email(request.getSession()).log("granting full CRUD instance permissions to user");
        return new CRUD().permit(CREATE, READ, UPDATE, DELETE);
    }

    public static CRUD grantServiceAccountInstanceCreateReadUpdateDelete(GetPermissionsRequest request,
                                                                        ServiceAccount serviceAccount) {
        info().serviceAccountID(serviceAccount)
                .datasetID(request.getDatasetID())
                .log("granting full CRUD instance permissions to service account");
        return new CRUD().permit(CREATE, READ, UPDATE, DELETE);
    }
}
