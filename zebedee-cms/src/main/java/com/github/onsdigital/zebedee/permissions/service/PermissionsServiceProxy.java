package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;

import java.io.IOException;
import java.util.List;
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

    public PermissionsServiceProxy(boolean jwtSessionsEnabled,
                                   PermissionsService legacyPermissionsService,
                                   PermissionsService jwtPermissionsService) {

        this.legacyPermissionsService = legacyPermissionsService;
        this.jwtPermissionsService = jwtPermissionsService;
        this.jwtSessionsEnabled = jwtSessionsEnabled;
    }

    /**
     * @param session {@link Session} to get the user details from.
     * @return
     * @throws IOException
     */
    @Override
    public boolean isPublisher(Session session) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.isPublisher(session);
        return legacyPermissionsService.isPublisher(session);
    }

    /**
     * @param email the email of the user to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean isPublisher(String email) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.isPublisher(email);
        return legacyPermissionsService.isPublisher(email);

    }

    /**
     * @param session {@link Session} to get the user details from.
     * @return
     * @throws IOException
     */
    @Override
    public boolean isAdministrator(Session session) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.isAdministrator(session);
        return legacyPermissionsService.isAdministrator(session);

    }

    /**
     * @param email the email of the user to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean isAdministrator(String email) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.isAdministrator(email);
        return legacyPermissionsService.isAdministrator(email);
    }

    /**
     * @param collection the collection to check users against. not used will always use legacy Permissions service
     * @return
     * @throws IOException
     */
    @Override
    public List<User> getCollectionAccessMapping(Collection collection) throws IOException {
        return legacyPermissionsService.getCollectionAccessMapping(collection);
    }

    /**
     * @return
     * @throws IOException
     */
    @Override
    public boolean hasAdministrator() throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.hasAdministrator();
        return legacyPermissionsService.hasAdministrator();

    }

    /**
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the user granting the permission.
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) jwtPermissionsService.addAdministrator(email, session);
        legacyPermissionsService.addAdministrator(email, session);

    }

    /**
     * @param email   the email of the user to remove the permission from.
     * @param session the {@link Session} of the user revoking the permission.
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) jwtPermissionsService.removeAdministrator(email, session);
        legacyPermissionsService.removeAdministrator(email, session);

    }

    /**
     * @param session the {@link Session} of the user to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canEdit(Session session) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.canEdit(session);
        return legacyPermissionsService.canEdit(session);

    }

    /**
     * @param email the email of the user to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canEdit(String email) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.canEdit(email);
        return legacyPermissionsService.canEdit(email);

    }

    /**
     * @param user the {@link User} to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canEdit(User user) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.canEdit(user);
        return legacyPermissionsService.canEdit(user);

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
        if (jwtSessionsEnabled) jwtPermissionsService.addEditor(email, session);
        legacyPermissionsService.addEditor(email, session);

    }

    /**
     * @param email   the email of the user to revoke the permission from.
     * @param session the {@link Session} of the {@link User} revoking the permissison.
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) jwtPermissionsService.removeEditor(email, session);
        legacyPermissionsService.removeEditor(email, session);

    }

    /**
     * @param session               the {@link Session} to get the user details from.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canView(Session session, CollectionDescription collectionDescription) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.canView(session, collectionDescription);
        return legacyPermissionsService.canView(session, collectionDescription);
    }

    /**
     * @param user                  the {@link User} to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canView(User user, CollectionDescription collectionDescription) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.canView(user, collectionDescription);
        return legacyPermissionsService.canView(user, collectionDescription);
    }

    /**
     * @param email                 the email of the user to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canView(String email, CollectionDescription collectionDescription) throws IOException {
        if (jwtSessionsEnabled) return jwtPermissionsService.canView(email, collectionDescription);
        return legacyPermissionsService.canView(email, collectionDescription);
    }

    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} in question.
     * @param team                  the {@link Team} to permit view permission to.
     * @param session               the {@link Session} of the user granting the permission.
     * @throws IOException
     * @throws ZebedeeException
     */
    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session)
            throws IOException, ZebedeeException {
        if (jwtSessionsEnabled) jwtPermissionsService.addViewerTeam(collectionDescription, team, session);
        legacyPermissionsService.addViewerTeam(collectionDescription, team, session);

    }

    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to get the viewer
     *                              teams for.
     * @param session               the {@link Session} of the {@link User} requesting this information.
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session)
            throws IOException, UnauthorizedException {
        return legacyPermissionsService.listViewerTeams(collectionDescription, session);
    }

    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to remove the team.
     * @param team                  the {@link Team} to remove.
     * @param session               the {@link Session} of the user revoking view permission.
     * @throws IOException
     * @throws ZebedeeException
     */
    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session)
            throws IOException, ZebedeeException {
        if (jwtSessionsEnabled) jwtPermissionsService.removeViewerTeam(collectionDescription, team, session);
        legacyPermissionsService.removeViewerTeam(collectionDescription, team, session);

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
        return legacyPermissionsService.userPermissions(email, session);
    }

    /**
     * this will always use legacy code
     *
     * @param t the team to check.
     * @return
     * @throws IOException
     */
    @Override
    public Set<String> listCollectionsAccessibleByTeam(Team t) throws IOException {
        return legacyPermissionsService.listCollectionsAccessibleByTeam(t);
    }

}