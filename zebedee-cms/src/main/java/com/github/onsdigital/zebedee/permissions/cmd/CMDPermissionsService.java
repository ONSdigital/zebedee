package com.github.onsdigital.zebedee.permissions.cmd;

/**
 * Definition of a service for getting user / service account CMD dataset permissions.
 */
public interface CMDPermissionsService {

    /**
     * Get a user's dataset permissions.<br/><br/>
     * <ul>
     * <li>
     * <i>Admin</i> and <i>Editor</i> users are granted full <b>CRUD</b> permissions for all datasets and collections.
     * </li>
     * <li>
     * <i>Viewer</i> users are only granted <b>R</b> permission if:
     * <ul>
     * <li>They are a member of a team assigned to the collection.</li>
     * <li>The collection contains the specified dataset</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param getPermissionsRequest {@link GetPermissionsRequest} specifies the request details - dataset ID,
     *                              Collection ID, Session ID.
     * @return {@link CRUD} permissions granted to that user. Returns an empty CRUD object for if the user is valid
     * but does
     * not have any permissions.
     * @throws PermissionsException problem getting the user permissions.
     */
    CRUD getUserDatasetPermissions(GetPermissionsRequest getPermissionsRequest) throws PermissionsException;

    /**
     * Get dataset permissions granted to a service account.
     *
     * @param getPermissionsRequest {@link GetPermissionsRequest} specifies the request details - service token and
     *                              dataset ID.
     * @return {@link CRUD} permissions granted to that service account. Returns an empty CRUD object for if the
     * service account is valid but does not have any permissions.
     * @throws PermissionsException problem getting the service permissions.
     */
    CRUD getServiceDatasetPermissions(GetPermissionsRequest getPermissionsRequest) throws PermissionsException;
}