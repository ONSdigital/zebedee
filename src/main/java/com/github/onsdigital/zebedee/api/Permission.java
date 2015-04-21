package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.json.Session;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.HashSet;

/**
 * Created by david on 12/03/2015.
 */
@Api
public class Permission {

    /**
     * Sets a user's password
     *
     * @param request     Should be a {@link PermissionDefinition} Json message.
     * @param response    <ul>
     *                    <li>If setting the password succeeds: a 200 OK message.</li>
     *                    <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                    <li>If the logged in user is not an administrator:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                    <li>If the password is not updated for any other reason:  {@link HttpStatus#BAD_REQUEST_400}.</li>
     *                    </ul>
     * @param permissionDefinition The email and permission details for the user.
     * @return A String message confirming that the user's password was updated.
     * @throws IOException
     */
    @POST
    public String setPermission(HttpServletRequest request, HttpServletResponse response, PermissionDefinition permissionDefinition) throws IOException {

        // Check the user session
        Session session = Root.zebedee.sessions.get(request);
        if (!Root.zebedee.permissions.isAdministrator(session.email)) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return "Unauthorised.";
        }

        // Check the request
        if (permissionDefinition == null) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Please provide a permission definition.";
        }

        // Check the request
        if (!Root.zebedee.users.exists(permissionDefinition.email)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Please provide a valid user email address.";
        }

        // Administrator
        if (permissionDefinition.admin) {
            Root.zebedee.permissions.addAdministrator(permissionDefinition.email, session);
        } else {
            Root.zebedee.permissions.removeAdministrator(permissionDefinition.email, session);
        }

        // Digital publishing
        if (permissionDefinition.editor) {
            Root.zebedee.permissions.addEditor(permissionDefinition.email, session);
        } else {
            Root.zebedee.permissions.removeEditor(permissionDefinition.email, session);
        }

        // Content owner
        if (permissionDefinition.contentOwnerPaths != null && permissionDefinition.contentOwnerPaths.size()>0) {
            Root.zebedee.permissions.addViewer(permissionDefinition.email, new HashSet<String>(permissionDefinition.contentOwnerPaths), session);
        } else {
            Root.zebedee.permissions.removeViewer(permissionDefinition.email, session);
        }

        return "Permissions updated for " + permissionDefinition.email;
    }

}
