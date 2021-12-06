package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

public class JWTPermissionsServiceImpl implements PermissionsService {
    static final String PUBLISHER_PERMISSIONS = "publisher";
    static final String ADMIN_PERMISSIONS = "admin";
    static final String JWTPERMISSIONSSERVICE_ERROR =
            "error accessing JWTPermissions Service";
    private static final ThreadLocal<UserDataPayload> store = new ThreadLocal<>();
    private Sessions sessionsService;

    public JWTPermissionsServiceImpl(Sessions sessionService) {
        this.sessionsService = sessionsService;
    }

    @Override
    public boolean isPublisher(Session session) throws JWTVerificationException {
        // Get JWT from JWT session service and check if the user has the 'Publisher' permission in their groups.
        return session != null && !StringUtils.isEmpty(session.getEmail()) &&
                hasPermission(session, PUBLISHER_PERMISSIONS);
    }

    @Override
    public boolean isPublisher(String email) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);

    }

    @Override
    public boolean isAdministrator(Session session) throws JWTVerificationException {
        // Get JWT from JWT session service and check if the user has the 'Admin' permission in their groups.
        return session != null && !StringUtils.isEmpty(session.getEmail()) &&
                hasPermission(session, ADMIN_PERMISSIONS);
    }

    public boolean isAdministrator(String email) throws JWTVerificationException {
        if (StringUtils.isEmpty(email)) {
            return false;
        }

        try {
            Session s = getSessionfromEmail(email);
            if (!hasPermission(s, ADMIN_PERMISSIONS)) {
                return false;
            }

        } catch (Exception exception) {
            return false;
        }

        return true;
    }

    @Override
    public List<User> getCollectionAccessMapping(Collection collection) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean hasAdministrator() throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public void addAdministrator(String email, Session session) throws JWTVerificationException, UnauthorizedException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public void removeAdministrator(String email, Session session) throws JWTVerificationException, UnauthorizedException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean canEdit(Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean canEdit(String email) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean canEdit(User user) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public void addEditor(String email, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public void removeEditor(String email, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean canView(Session session, CollectionDescription collectionDescription) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean canView(User user, CollectionDescription collectionDescription) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public boolean canView(String email, CollectionDescription collectionDescription) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public Set<String> listCollectionsAccessibleByTeam(Team t) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }


    public boolean hasPermission(Session session, String permission) {
        try {
            return ArrayUtils.contains(session.getGroups(), permission);
        } catch (Exception exception) {
            return false;
        }
    }

    public Session getSessionfromEmail(String email) {
        try {
            return sessionsService.find(email);

        } catch (Exception exception) {

            return null;
        }
    }
}