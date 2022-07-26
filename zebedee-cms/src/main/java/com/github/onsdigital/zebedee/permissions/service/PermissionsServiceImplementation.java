package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.UserDataPayload;
import com.github.onsdigital.dp.authorisation.permissions.PermissionChecker;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.session.model.Session;
import org.joda.time.Duration;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.text.MessageFormat.format;

public class PermissionsServiceImplementation implements PermissionsService {
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private static final String UNSUPPORTED_ERROR = "New Permissions API is enabled: {0} is no longer supported";
    private PermissionChecker permissionChecker;

    public PermissionsServiceImplementation(String permissionsAPIHost ) {
        this.permissionChecker = new PermissionChecker(permissionsAPIHost, Duration.standardSeconds(10), Duration.standardSeconds(20), Duration.standardMinutes(30));
    }

    public PermissionsServiceImplementation() {
        this.permissionChecker = new PermissionChecker("", Duration.standardSeconds(10), Duration.standardSeconds(20), Duration.standardMinutes(30));
    }


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
        boolean has_permission = false;
        try {
            has_permission = hasPermission(session, "legacy:edit", "");

        } catch (Exception e){
            return false;
        }
        return has_permission;    }


    @Override
    public void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "addEditor"));

    }

    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "removeEditor"));

    }

    @Override
    public boolean canView(Session session, String collectionId) throws IOException {
        boolean has_permission = false;
        readLock.lock();
        try {
            has_permission = hasPermission(session, "legacy:read", collectionId);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
        return has_permission;
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
        readLock.lock();
        try {
            boolean isEditor = canEdit(session);
            boolean isAdmin = canUpdate(session);

            return new PermissionDefinition()
                    .isAdmin(isAdmin)
                    .isEditor(isEditor);
        } finally {
            readLock.unlock();
        }
    }

    private Boolean hasPermission(Session session, String permissionString, String collectionId) throws Exception {
        HashMap<String, String> attributes = new HashMap<String, String>() {{
            put("collection_id", collectionId);
        }};


        UserDataPayload userDataPayload =  new UserDataPayload(session.getId(),session.getEmail(), Arrays.asList(new String[]{"admin"}));

        Boolean hasPermission = permissionChecker.hasPermission(userDataPayload, permissionString, attributes);

        return hasPermission;
    }
    private boolean canUpdate(Session session) throws IOException {
        boolean has_permission = false;
        readLock.lock();
        try {
            has_permission = hasPermission(session, "users:update", "");

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
        return has_permission;
    }

}
