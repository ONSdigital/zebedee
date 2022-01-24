package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.service.UsersService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;

@Api
public class Permission {

    private Sessions sessionsService;
    private PermissionsService permissionsService;
    private UsersService usersService;
    private CollectionKeyring collectionKeyring;
    private Collections collections;

    public Permission() {
        this.sessionsService = Root.zebedee.getSessions();
        this.permissionsService = Root.zebedee.getPermissionsService();
        this.usersService = Root.zebedee.getUsersService();
        this.collectionKeyring = Root.zebedee.getCollectionKeyring();
        this.collections = Root.zebedee.getCollections();
    }

    Permission(final Sessions sessionsService, PermissionsService permissionsService, UsersService usersService,
               Collections collections, CollectionKeyring collectionKeyring) {
        this.sessionsService = sessionsService;
        this.permissionsService = permissionsService;
        this.usersService = usersService;
        this.collections = collections;
        this.collectionKeyring = collectionKeyring;
    }

    /**
     * Grants the specified permissions.
     *
     * This endpoint is used by the user management screens to manage the role of the users.
     *
     * @param request              Should be a {@link PermissionDefinition} Json message.
     * @param response             <ul>
     *                             <li>If admin is True, grants administrator permission. If admin is False, revokes</li>
     *                             <li>If editor is True, grants editing permission. If editor is False, revokes</li>
     *                             <li>Note that admins automatically get editor permissions</li>
     *                             </ul>
     * @param permissionDefinition The email and permission details for the user.
     * @return A String message confirming that the user's permissions were updated.
     * @throws IOException           If an error occurs accessing data.
     * @throws UnauthorizedException If the logged in user is not an administrator.
     * @throws BadRequestException   If the user specified in the {@link PermissionDefinition} is not found.
     *
     * @deprecated by the move to JWT sessions and will be removed after the migration is complete.
     *
     * // TODO: Remove this endpoint once the JWT sessions have been enabled as this will mean user management has moved
     *          to the dp-identity-api
     */
    @Deprecated
    @POST
    public String grantPermission(HttpServletRequest request, HttpServletResponse response,
                                  PermissionDefinition permissionDefinition) throws IOException, ZebedeeException {

        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            throw new NotFoundException("JWT sessions are enabled: POST /permission is no longer supported");
        }

        Session session = getSession(request);

        // Assign / remove admin permissions
        if (permissionDefinition.isAdmin()) {
            assignAdminPermissionsToUser(request, permissionDefinition, session);

            // Admins must be publishers so update the permissions accordingly
            permissionDefinition.isEditor(true);
        } else {
            removeAdminPermissionsFromUser(request, permissionDefinition, session);
        }

        // Assign / remove editor permissions
        if (permissionDefinition.isEditor()) {
            addEditorPermissionToUser(request, permissionDefinition, session);
        } else {
            removeEditorPermissionFromUser(request, permissionDefinition, session);
        }

        return "Permissions updated for " + permissionDefinition.getEmail();
    }

    /**
     * Grants the specified permissions.
     *
     * @param request  Should be of the form {@code /permission?email=florence@example.com}
     * @param response A permissions object for that user
     * @return
     * @throws IOException           If an error occurs accessing data.
     * @throws UnauthorizedException If the user is not an administrator.
     * @throws BadRequestException   If the user specified in the {@link PermissionDefinition} is not found.
     *
     * This endpoint is called by florence in two places:
     *    - On login to determine whether the calling user is a viewer, publisher or admin
     *    - When editing a user, this endpoint is called to load whether they are a viewer, publisher or admin to display
     *      on the user admin screens
     *
     * // TODO: Update this endpoint once the JWT sessions have been enabled to remove the ability to check the permissions
     *          of another user (i.e. case 2 in the florence usages above). This basically means removing the ability to
     *          pass the `email` param.
     */
    @GET
    public PermissionDefinition getPermissions(HttpServletRequest request, HttpServletResponse response)
            throws IOException, NotFoundException, UnauthorizedException, BadRequestException {

        Session session = sessionsService.get(request);
        String email = request.getParameter("email");

        PermissionDefinition permissionDefinition;
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            if (email != null) {
                throw new BadRequestException("invalid parameter 'email': checking the permissions of another user is no longer supported");
            }
            permissionDefinition = permissionsService.userPermissions(session);
        } else {
            permissionDefinition = permissionsService.userPermissions(email, session);
        }

        return permissionDefinition;
    }

    private void assignAdminPermissionsToUser(HttpServletRequest request, PermissionDefinition permissionDefinition,
                                              Session session)
            throws IOException, UnauthorizedException {
        permissionsService.addAdministrator(permissionDefinition.getEmail(), session);
        Audit.Event.ADMIN_PERMISSION_ADDED
                .parameters()
                .host(request)
                .actionedByEffecting(session.getEmail(), permissionDefinition.getEmail())
                .log();
    }

    private void removeAdminPermissionsFromUser(HttpServletRequest request, PermissionDefinition permissionDefinition,
                                                Session session)
            throws IOException, UnauthorizedException {
        permissionsService.removeAdministrator(permissionDefinition.getEmail(), session);

        Audit.Event.ADMIN_PERMISSION_REMOVED
                .parameters()
                .host(request)
                .actionedByEffecting(session.getEmail(), permissionDefinition.getEmail())
                .log();
    }

    private void addEditorPermissionToUser(HttpServletRequest request, PermissionDefinition permissionDefinition,
                                           Session session)
            throws IOException, UnauthorizedException, BadRequestException, NotFoundException {
        permissionsService.addEditor(permissionDefinition.getEmail(), session);

        Audit.Event.PUBLISHER_PERMISSION_ADDED
                .parameters()
                .host(request)
                .actionedByEffecting(session.getEmail(), permissionDefinition.getEmail())
                .log();
    }

    private void removeEditorPermissionFromUser(HttpServletRequest request, PermissionDefinition permissionDefinition,
                                                Session session)
            throws IOException, UnauthorizedException {
        permissionsService.removeEditor(permissionDefinition.getEmail(), session);

        Audit.Event.PUBLISHER_PERMISSION_REMOVED
                .parameters()
                .host(request)
                .actionedByEffecting(session.getEmail(), permissionDefinition.getEmail())
                .log();
    }

    private Session getSession(HttpServletRequest request) throws InternalServerError {
        Session session = null;
        try {
            session = sessionsService.get(request);
        } catch (IOException ex) {
            throw new InternalServerError("error getting user session", ex);
        }

        if (session == null) {
            throw new InternalServerError("error expected user session but was null");
        }

        return session;
    }
}
