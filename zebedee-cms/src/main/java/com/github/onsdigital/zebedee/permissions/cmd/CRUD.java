package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;


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

    public static CRUD permitServiceAccountCreateReadUpdateDelete(ServiceAccount serviceAccount, String datasetID) {
        info().serviceAccountID(serviceAccount.getId())
                .datasetID(datasetID)
                .log("granting full CRUD permissions to service account");
        return new CRUD().permit(CREATE, READ, UPDATE, DELETE);
    }

    public static CRUD permitUserCreateReadUpdateDelete(String collectionID, String datasetID, Session session) {
        info().collectionID(collectionID).datasetID(datasetID).email(session)
                .log("granting full CRUD permissions to user");
        return new CRUD().permit(CREATE, READ, UPDATE, DELETE);
    }

    public static CRUD permitUserRead(String collectionID, String datasetID, Session session) {
        info().collectionID(collectionID).datasetID(datasetID).email(session)
                .log("granting READ permission to user");
        return new CRUD().permit(READ);
    }

    public static CRUD permitUserNone(String collectionID, String datasetID, Session session, String message) {
        info().collectionID(collectionID).datasetID(datasetID).email(session).log(message);
        return new CRUD();
    }
}
