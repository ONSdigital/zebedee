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
     * @param request {@link GetPermissionsRequest} specifies the request details - dataset ID,
     *                              Collection ID, Session ID.
     * @return {@link CRUD} permissions granted to that user. Returns an empty CRUD object for if the user is valid
     * but does
     * not have any permissions.
     * @throws PermissionsException problem getting the user permissions.
     */
    CRUD getUserDatasetPermissions(GetPermissionsRequest request) throws PermissionsException;

    /**
     * Get dataset permissions granted to a service account.
     *
     * @param request {@link GetPermissionsRequest} specifies the request details - service token and
     *                              dataset ID.
     * @return {@link CRUD} permissions granted to that service account. Returns an empty CRUD object for if the
     * service account is valid but does not have any permissions.
     * @throws PermissionsException problem getting the service permissions.
     */
    CRUD getServiceDatasetPermissions(GetPermissionsRequest request) throws PermissionsException;


    /**
     * Get a user's instance permissions. <i>Admin</i> and <i>Editor</i> users are granted full <b>CRUD</b>
     * permissions otherwuse no permissions are granted.
     *
     * @param request {@link GetPermissionsRequest} specifies the session ID of the user to get the
     *                              permissions for.
     * @return {@link CRUD} permissions granted to the user.
     * @throws PermissionsException problem getting the user permissions.
     */
    CRUD getUserInstancePermissions(GetPermissionsRequest request) throws PermissionsException;


    /**
     * Get a service accounts instance permissions.
     *
     * @param request {@link GetPermissionsRequest} specifies the service account to get the instance
     *                              permissions for.
     * @return {@link CRUD} permissions granted to the service account.
     * @throws PermissionsException problem getting the permissions.
     */
    CRUD getServiceInstancePermissions(GetPermissionsRequest request) throws PermissionsException;
}
