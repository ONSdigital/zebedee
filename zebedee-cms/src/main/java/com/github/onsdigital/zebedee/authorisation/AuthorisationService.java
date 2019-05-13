package com.github.onsdigital.zebedee.authorisation;


/**
 * ServiceAccount provides methods for determining user identity and authorisation.
 */
public interface AuthorisationService {

    /**
     * Get the {@link UserIdentity} for the user the specifed sessionID belongs to.
     *
     * @param sessionID the sessionID to get the user identity from
     * @return the users Authenticate if the sessionID is valid, the user exists and is authenticated.
     * @throws UserIdentityException if there is any problem determining the user identity.
     */
    UserIdentity identifyUser(String sessionID) throws UserIdentityException;

    /**
     * Get the user's permissions for the specified dataset.
     *
     * @param sessionID    the requesting user's session ID.
     * @param datasetID    the dataset to get the permissions for.
     * @param collectionID the collection ID the dataset is part of.
     * @return {@link DatasetPermissions}
     * @throws DatasetPermissionsException a problem getting the permissions.
     */
    DatasetPermissions getUserPermissions(String sessionID, String datasetID, String collectionID) throws DatasetPermissionsException;

    DatasetPermissions getServicePermissions(String serviceToken) throws DatasetPermissionsException;
}
