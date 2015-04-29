package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
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
     *                             </ul>
     * @param permissionDefinition The email and permission details for the user.
     * @return A String message confirming that the user's permissions were updated.
     * @throws IOException           If an error occurs accessing data.
     * @throws UnauthorizedException If the logged in user is not an administrator.
     * @throws BadRequestException   If the user specified in the {@link PermissionDefinition} is not found.
     */
    @POST
    public String grantPermission(HttpServletRequest request, HttpServletResponse response, PermissionDefinition permissionDefinition) throws IOException, UnauthorizedException, NotFoundException {

        Session session = Root.zebedee.sessions.get(request);

        // Administrator
        if (BooleanUtils.isTrue(permissionDefinition.admin)) {
            Root.zebedee.permissions.addAdministrator(permissionDefinition.email, session);
        } else if (BooleanUtils.isFalse(permissionDefinition.admin)) {
            Root.zebedee.permissions.removeAdministrator(permissionDefinition.email, session);
        }

        // Digital publishing
        if (BooleanUtils.isTrue(permissionDefinition.editor)) {
            Root.zebedee.permissions.addEditor(permissionDefinition.email, session);
        } else if (BooleanUtils.isFalse(permissionDefinition.editor)) {
            Root.zebedee.permissions.removeEditor(permissionDefinition.email, session);
        }

        return "Permissions updated for " + permissionDefinition.email;
    }

}
