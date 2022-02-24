package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;

import java.io.IOException;
import java.util.Set;

/**
 * A proxy class that sits in front of the 2 permissions service implementations (legacy & JWT). The class does not
 * perform any permisisons logic it determines which instance to invoke (legacy or JWT) based on the value of the
 * feature flag.
 */
public class PermissionsServiceProxy implements PermissionsService {

    private final PermissionsService legacyPermissionsService;
    private final PermissionsService jwtPermissionsService;
    private final boolean jwtSessionsEnabled;

    public PermissionsServiceProxy(PermissionsService legacyPermissionsService,
                                   PermissionsService jwtPermissionsService,
                                   boolean jwtSessionsEnabled) {

        this.legacyPermissionsService = legacyPermissionsService;
        this.jwtPermissionsService = jwtPermissionsService;
        this.jwtSessionsEnabled = jwtSessionsEnabled;
    }

    /**
     * @param session {@link Session} to get the user details from.
     * @return the required module on jwtSessionsEnabled
     * @throws IOException from required module
     */
    @Override
    public boolean isPublisher(Session session) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.isPublisher(session);
        }
        return legacyPermissionsService.isPublisher(session);
    }

    /**
     * @param session {@link Session} to get the user details from.
     * @return the required module on jwtSessionsEnabled
     * @throws IOException from required module
     */
    @Override
    public boolean isAdministrator(Session session) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.isAdministrator(session);
        }
        return legacyPermissionsService.isAdministrator(session);
    }

    /**
     * @return hasAdmin
     * @throws IOException the required module on jwtSessionsEnabled
     */
    @Override
    public boolean hasAdministrator() throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.hasAdministrator();
        }
        return legacyPermissionsService.hasAdministrator();
    }

    /**
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the user granting the permission.
     * @throws IOException           the required module on jwtSessionsEnabled
     * @throws UnauthorizedException
     */
    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) {
            jwtPermissionsService.addAdministrator(email, session);
        } else {
            legacyPermissionsService.addAdministrator(email, session);
        }
    }

    /**
     * @param email   the email of the user to remove the permission from.
     * @param session the {@link Session} of the user revoking the permission.
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) {
            jwtPermissionsService.removeAdministrator(email, session);
            return;
        }
        legacyPermissionsService.removeAdministrator(email, session);
    }

    /**
     * @param session the {@link Session} of the user to check.
     * @return the required module on jwtSessionsEnabled
     * @throws IOException
     */
    @Override
    public boolean canEdit(Session session) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.canEdit(session);
        }
        return legacyPermissionsService.canEdit(session);
    }

    /**
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the {@link User} granting the permissison.
     * @throws IOException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    @Override
    public void addEditor(String email, Session session)
            throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        if (jwtSessionsEnabled) {
            jwtPermissionsService.addEditor(email, session);
        } else {
            legacyPermissionsService.addEditor(email, session);
        }
    }

    /**
     * @param email   the email of the user to revoke the permission from.
     * @param session the {@link Session} of the {@link User} revoking the permissison.
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) {
            jwtPermissionsService.removeEditor(email, session);
        } else {
            legacyPermissionsService.removeEditor(email, session);
        }
    }

    /**
     * @param session      the {@link Session} to get the user details from.
     * @param collectionId the ID of the {@link Collection} to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canView(Session session, String collectionId) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.canView(session, collectionId);
        }
        return legacyPermissionsService.canView(session, collectionId);
    }

    /**
     * Set the list of team IDs that are allowed viewer access to a collection
     *
     * @param collectionId    the ID of the collection collection to set viewer permissions for.
     * @param collectionTeams the set of team IDs for which viewer permissions should be granted to the collection.
     * @param session         the session of the user that is attempting to set the viewer permissions.
     * @throws IOException           if reading or writing the access mapping fails.
     * @throws UnauthorizedException if the users' session isn't authorised to edit collections.
     * @deprecated this is deprecated in favour of the dp-permissions-api and will be removed once full migration to
     * the new API is complete.
     * <p>
     * TODO: Remove once migration to dp-permissions-api is complete and the accessmapping is being removed.
     */
    @Deprecated
    public void setViewerTeams(Session session, String collectionId, Set<String> collectionTeams)
            throws IOException, ZebedeeException {
        if (jwtSessionsEnabled) {
            jwtPermissionsService.setViewerTeams(session, collectionId, collectionTeams);
        } else {
            legacyPermissionsService.setViewerTeams(session, collectionId, collectionTeams);
        }
    }

    /**
     * @param session the {@link Session} of the {@link User} requesting this information.
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public Set<String> listViewerTeams(Session session, String collectionId)
            throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.listViewerTeams(session, collectionId);
        }
        return legacyPermissionsService.listViewerTeams(session, collectionId);
    }

    /**
     * @param email   the email of the user to get the {@link PermissionDefinition} for.
     * @param session the {@link Session} of the user requesting the {@link PermissionDefinition}.
     * @return
     * @throws IOException
     * @throws NotFoundException
     * @throws UnauthorizedException
     */
    @Override
    public PermissionDefinition userPermissions(String email, Session session)
            throws IOException, NotFoundException, UnauthorizedException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.userPermissions(email, session);
        }
        return legacyPermissionsService.userPermissions(email, session);
    }

    @Override
    public PermissionDefinition userPermissions(Session session) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.userPermissions(session);
        }
        return legacyPermissionsService.userPermissions(session);
    }
}