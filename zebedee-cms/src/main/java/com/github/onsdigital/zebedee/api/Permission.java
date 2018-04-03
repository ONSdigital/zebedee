package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.session.model.Session;
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

        Session session = Root.zebedee.getSessionsService().get(request);

        // Administrator
        if (BooleanUtils.isTrue(permissionDefinition.isAdmin())) {
            Root.zebedee.getPermissionsService().addAdministrator(permissionDefinition.getEmail(), session);
            // Admins must be publishers so update the permissions accordingly
            permissionDefinition.isEditor(true);
            Audit.Event.ADMIN_PERMISSION_ADDED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), permissionDefinition.getEmail())
                    .log();
        } else if (BooleanUtils.isFalse(permissionDefinition.isAdmin())) {
            Root.zebedee.getPermissionsService().removeAdministrator(permissionDefinition.getEmail(), session);
            Audit.Event.ADMIN_PERMISSION_REMOVED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), permissionDefinition.getEmail())
                    .log();
        }

        // Digital publishing
        if (BooleanUtils.isTrue(permissionDefinition.isEditor())) {
            Root.zebedee.getPermissionsService().addEditor(permissionDefinition.getEmail(), session);
            Audit.Event.PUBLISHER_PERMISSION_ADDED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), permissionDefinition.getEmail())
                    .log();
        } else if (BooleanUtils.isFalse(permissionDefinition.isEditor())) {
            Root.zebedee.getPermissionsService().removeEditor(permissionDefinition.getEmail(), session);
            Audit.Event.PUBLISHER_PERMISSION_REMOVED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), permissionDefinition.getEmail())
                    .log();
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

        Session session = Root.zebedee.getSessionsService().get(request);
        String email = request.getParameter("email");

        PermissionDefinition permissionDefinition = Root.zebedee.getPermissionsService().userPermissions(email, session);

        return permissionDefinition;
    }

}
