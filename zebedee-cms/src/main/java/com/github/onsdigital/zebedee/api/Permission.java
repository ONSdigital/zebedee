package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.apache.commons.lang3.BooleanUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by david on 12/03/2015.
 */
@Api
public class Permission {

    private Sessions sessionsService;
    private PermissionsService permissionsService;

    public Permission() {
        this.sessionsService = Root.zebedee.getSessions();
        this.permissionsService = Root.zebedee.getPermissionsService();
    }

    Permission(final Sessions sessionsService, PermissionsService permissionsService) {
        this.sessionsService = sessionsService;
        this.permissionsService = permissionsService;
    }

    /**
     * Grants the specified permissions.
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
     */
    @POST
    public String grantPermission(HttpServletRequest request, HttpServletResponse response, PermissionDefinition permissionDefinition)
            throws IOException, ZebedeeException {

        Session session = getSession(request);

        // Assign / remove admin permissions
        if (permissionDefinition.isAdmin()) {
            assignAdminPermissionsToUser(request, permissionDefinition, session);
            // TODO Assign keys to to new user (this was previously done inside permissions service).
        } else {
            removeAdminPermissionsFromUser(request, permissionDefinition, session);
        }

        // Assign / remove editor permissions
        if (permissionDefinition.isEditor()) {
            addEditorPermissionToUser(request, permissionDefinition, session);
            // TODO Assign keys to to new user.
        } else  {
            removeEditorPermissionToUser(request, permissionDefinition, session);
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
     */
    @GET
    public PermissionDefinition getPermissions(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, UnauthorizedException {

        Session session = Root.zebedee.getSessions().get(request);
        String email = request.getParameter("email");

        PermissionDefinition permissionDefinition = Root.zebedee.getPermissionsService().userPermissions(email, session);

        return permissionDefinition;
    }

    private void assignAdminPermissionsToUser(HttpServletRequest request, PermissionDefinition permissionDefinition,
                                              Session session)
            throws IOException, UnauthorizedException {
        permissionsService.addAdministrator(permissionDefinition.getEmail(), session);

        // Admins must be publishers so update the permissions accordingly
        permissionDefinition.isEditor(true);
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

    private void removeEditorPermissionToUser(HttpServletRequest request, PermissionDefinition permissionDefinition,
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
