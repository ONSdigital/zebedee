package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.session.model.Session;

import java.io.IOException;
import java.util.Set;

import static java.text.MessageFormat.format;

public class NewPermissionsServiceImpl implements PermissionsService {

    private static final String UNSUPPORTED_ERROR = "New Permissions API is enabled: {0} is no longer supported";


    @Override
    public boolean isPublisher(Session session) throws IOException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "isPublisher"));
    }

    @Override
    public boolean isAdministrator(Session session) throws IOException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "isAdministrator"));
    }

    @Override
    public boolean hasAdministrator() throws IOException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "hasAdministrator"));
    }

    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "addAdministrator"));

    }

    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "removeAdministrator"));

    }

    @Override
    public boolean canEdit(Session session) throws IOException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "canEdit"));
    }


    @Override
    public void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "addEditor"));

    }

    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "removeEditor"));

    }

    @Override
    public boolean canView(Session session, String collectionId) throws IOException{
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "listViewerTeams"));
    }

    @Override
    public Set<String> listViewerTeams(Session session, String collectionId) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "listViewerTeams"));
    }

    @Override
    public void setViewerTeams(Session session, String collectionId, Set<String> collectionTeams) throws IOException, ZebedeeException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "setViewerTeams"));

    }

    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "userPermissions"));
    }

    @Override
    public PermissionDefinition userPermissions(Session session) throws IOException {
        return null;
    }

}
