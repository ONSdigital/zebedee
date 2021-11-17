package com.github.onsdigital.zebedee.permissions.service;

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

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class JWTPermissionsServiceImpl implements PermissionsService {

    private Sessions sessions;

    /**
     *
     * @param sessions
     */
    public JWTPermissionsServiceImpl(Sessions sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean isPublisher(Session session) throws IOException {
        // Get JWT from JWT session service and check if the user has the 'Publisher' permission in their groups.
        return false;
    }

    @Override
    public boolean isPublisher(String email) throws IOException {
        // Get JWT from JWT session service and check if the user has the 'Publisher' permission in their groups.
        return false;
    }

    @Override
    public boolean isAdministrator(Session session) throws IOException {
        // Get JWT from JWT session service and check if the user has the 'Admin' permission in their groups.
        return false;
    }

    @Override
    public boolean isAdministrator(String email) throws IOException {
        // Get JWT from JWT session service and check if the user has the 'Admin' permission in their groups.
        return false;
    }

    @Override
    public List<User> getCollectionAccessMapping(Collection collection) throws IOException {
        // Copy and past from legacy impl
        return null;
    }

    @Override
    public boolean hasAdministrator() throws IOException {
        return false;
    }

    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {

    }

    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {

    }

    @Override
    public boolean canEdit(Session session) throws IOException {
        return false;
    }

    @Override
    public boolean canEdit(String email) throws IOException {
        return false;
    }

    @Override
    public boolean canEdit(User user) throws IOException {
        return false;
    }

    @Override
    public void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

    }

    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {

    }

    @Override
    public boolean canView(Session session, CollectionDescription collectionDescription) throws IOException {
        return false;
    }

    @Override
    public boolean canView(User user, CollectionDescription collectionDescription) throws IOException {
        return false;
    }

    @Override
    public boolean canView(String email, CollectionDescription collectionDescription) throws IOException {
        return false;
    }

    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException {

    }

    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException {

    }

    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException, UnauthorizedException {
        return null;
    }

    @Override
    public Set<String> listCollectionsAccessibleByTeam(Team t) throws IOException {
        return null;
    }
}
