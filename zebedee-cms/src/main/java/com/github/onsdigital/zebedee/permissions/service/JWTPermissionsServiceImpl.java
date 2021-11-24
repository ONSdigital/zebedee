package com.github.onsdigital.zebedee.permissions.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;

public class JWTPermissionsServiceImpl implements PermissionsService {
    static final String JWTPERMISSIONSSERVICE_ERROR =
            "error accessing JWTPermissions Service";
    private Session session;
    private Sessions sessions;

    /**
     *
     * @param sessions
     */
    public JWTPermissionsServiceImpl(Session session) {
        this.session = session;
    }

    @Override
    public boolean isPublisher(Session session) throws IOException {
        // Get JWT from JWT session service and check if the user has the 'Publisher' permission in their groups.
        throw new IOException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean isPublisher(String email) throws IOException {
        // Get JWT from JWT session service and check if the user has the 'Publisher' permission in their groups.
        throw new IOException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean isAdministrator(Session session) throws IOException {
        // Get JWT from JWT session service and check if the user has the 'Admin' permission in their groups.
        throw new IOException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean isAdministrator(String email) throws IOException {
        // Get JWT from JWT session service and check if the user has the 'Admin' permission in their groups.
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public List<User> getCollectionAccessMapping(Collection collection) throws IOException {
        // Copy and past from legacy impl
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public boolean hasAdministrator() throws IOException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public boolean canEdit(Session session) throws IOException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public boolean canEdit(String email) throws IOException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public boolean canEdit(User user) throws IOException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public boolean canView(Session session, CollectionDescription collectionDescription) throws IOException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public boolean canView(User user, CollectionDescription collectionDescription) throws IOException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public boolean canView(String email, CollectionDescription collectionDescription) throws IOException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws IOException, UnauthorizedException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException, UnauthorizedException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }

    @Override
    public Set<String> listCollectionsAccessibleByTeam(Team t) throws IOException {
        throw new IOException( JWTPERMISSIONSSERVICE_ERROR );
    }


}