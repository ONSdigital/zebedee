package com.github.onsdigital.zebedee.permissions.store;

import com.github.onsdigital.zebedee.permissions.model.AccessMapping;

import java.io.IOException;

/**
 * @deprecated the files-on-disk access mapping is deprecated and will be removed once the migration of policy management
 *             to the dp-permissions-api has been completed
 *
 * // TODO: remove this interface once the authorisation migration to using the dp-permissions-api been completed
 */
public interface PermissionsStore {

    /**
     * @return
     * @throws IOException
     */
    AccessMapping getAccessMapping() throws IOException;

    /**
     * @param accessMapping
     * @throws IOException
     */
    void saveAccessMapping(AccessMapping accessMapping) throws IOException;
}
