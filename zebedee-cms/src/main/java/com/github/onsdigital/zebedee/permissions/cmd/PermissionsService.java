package com.github.onsdigital.zebedee.permissions.cmd;

/**
 * Definition of a service for getting user / service account CMD dataset permissions.
 */
public interface PermissionsService {

    /**
     * Get user dataset permissions for the specified collection and dataset combination.<br/><br/>
     * <i>Admin</i> and <i>Editor</i> users are granted full <b>CRUD</b> permissions for all datasets and collections.
     * <br/><br/>
     * <i>Viewer</i> users are only granted <b>R</b> permission if:
     * <ul>
     * <li>They are a member of a team assigned to the collection.</li>
     * <li><b>AND</b> the collection contains the specified dataset</li>
     * </ul>
     *
     * @param sessionID    the users session ID. Used to verify they are authenticated and to determined their user type
     *                     (admin, editor, viewer).
     * @param datasetID    the ID of the dataset to get the users permissions for.
     * @param collectionID the collection the requested dataset is in.
     * @return {@link CRUD} permissions for that user. Returns an empty CRUD object for if the user is valid but does
     * not have any permissions for thats collection & dataset.
     * @throws PermissionsException problem getting the user permissions.
     */
    CRUD getUserDatasetPermissions(String sessionID, String datasetID, String collectionID)
            throws PermissionsException;

    /**
     * Get dataset permissions for provided {@link com.github.onsdigital.zebedee.model.ServiceAccount}.
     *
     * @param serviceToken the service account id to check the permissions for.
     * @param datasetID    the dataset the permissions are granted for.
     * @return {@link CRUD} permissions for that service account. Returns an empty CRUD object for a valid service
     * with no permissions allowed.
     * @throws PermissionsException problem getting/checking the service account permissions.
     */
    CRUD getServiceDatasetPermissions(String serviceToken, String datasetID) throws PermissionsException;
}
