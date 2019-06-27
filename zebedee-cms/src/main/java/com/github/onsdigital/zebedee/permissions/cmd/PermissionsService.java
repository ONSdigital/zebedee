package com.github.onsdigital.zebedee.permissions.cmd;

public interface PermissionsService {

    CRUD getUserDatasetPermissions(String sessionID, String datasetID, String collectionID)
            throws PermissionsException;

    CRUD getServiceDatasetPermissions(String serviceToken) throws PermissionsException;
}
