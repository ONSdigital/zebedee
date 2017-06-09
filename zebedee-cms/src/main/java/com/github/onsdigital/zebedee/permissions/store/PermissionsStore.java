package com.github.onsdigital.zebedee.permissions.store;

import com.github.onsdigital.zebedee.permissions.model.AccessMapping;

import java.io.IOException;

/**
 * Created by dave on 31/05/2017.
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
