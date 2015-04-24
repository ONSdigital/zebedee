package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
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
     *                             <li>If admin is true, grants administrator permission.</li>
     *                             <li>If editor is true, grants editing permission.</li>
     *                             <li>If a team name is provided, grants membership of the matching team.</li>
     *                             </ul>
     * @param permissionDefinition The email and permission details for the user.
     * @return A String message confirming that the user's permissions were updated.
     * @throws IOException
     */
    @POST
    public String grantPermission(HttpServletRequest request, HttpServletResponse response, PermissionDefinition permissionDefinition) throws IOException {

        Session session = Root.zebedee.sessions.find(permissionDefinition.email);

        // Administrator
        if (permissionDefinition.admin) {
            Root.zebedee.permissions.addAdministrator(permissionDefinition.email, session);
        }

        // Digital publishing
        if (permissionDefinition.editor) {
            Root.zebedee.permissions.addEditor(permissionDefinition.email, session);
        }

        // Content owner
        if (StringUtils.isNotBlank(permissionDefinition.teamName)) {
            Team team = Root.zebedee.teams.findTeam(permissionDefinition.teamName);
            Root.zebedee.teams.addTeamMember(permissionDefinition.email, team, session);
        }

        return "Permissions updated for " + permissionDefinition.email;
    }

    /**
     * Revokes the specified permissions.
     *
     * @param request              Should be a {@link PermissionDefinition} Json message.
     * @param response             <ul>
     *                             <li>If admin is true, revokes administrator permission.</li>
     *                             <li>If editor is true, revokes editing permission.</li>
     *                             <li>If a team name is provided, revokes membership of the matching team.</li>
     *                             </ul>
     * @param permissionDefinition The email and permission details for the user.
     * @return A String message confirming that the user's permissions were updated.
     * @throws IOException
     */
    @DELETE
    public String revokePermission(HttpServletRequest request, HttpServletResponse response, PermissionDefinition permissionDefinition) throws IOException {

        Session session = Root.zebedee.sessions.find(permissionDefinition.email);

        // Administrator
        if (permissionDefinition.admin) {
            Root.zebedee.permissions.removeAdministrator(permissionDefinition.email, session);
        }

        // Digital publishing
        if (permissionDefinition.editor) {
            Root.zebedee.permissions.removeEditor(permissionDefinition.email, session);
        }

        // Content owner
        if (StringUtils.isNotBlank(permissionDefinition.teamName)) {
            Team team = Root.zebedee.teams.findTeam(permissionDefinition.teamName);
            Root.zebedee.teams.removeTeamMember(permissionDefinition.email, team, session);
        }

        return "Permissions updated for " + permissionDefinition.email;
    }

}
