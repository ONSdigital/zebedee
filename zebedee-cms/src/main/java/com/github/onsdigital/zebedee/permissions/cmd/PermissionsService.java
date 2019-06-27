package com.github.onsdigital.zebedee.permissions.cmd;

public interface PermissionsService {

    Permissions getUserDatasetPermissions(String sessionID, String datasetID, String collectionID)
            throws PermissionsException;

    Permissions getServiceDatasetPermissions(String serviceToken) throws PermissionsException;
}
