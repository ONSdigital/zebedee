package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;

public class JWTPermissionsServiceImpl implements PermissionsService {
    static final String PUBLISHER_PERMISSIONS = "publisher";
    static final String ADMIN_PERMISSIONS = "admin";
    static final String JWTPERMISSIONSSERVICE_ERROR =
            "error accessing JWTPermissions Service";
    private Sessions sessionsService;
    private Session session;


    /**
     * @param sessionService
     */

    public JWTPermissionsServiceImpl(Sessions sessionService) {
        this.sessionsService = sessionsService;
    }

    /**
     * @param session {@link Session} to get the user details from.
     * @return boolean
     * @throws JWTVerificationException
     */
    @Override
    public boolean isPublisher(Session session) throws JWTVerificationException {
        // Get JWT from JWT session service and check if the user has the 'Publisher' permission in their groups.
        return session != null && !StringUtils.isEmpty(session.getEmail()) &&
                hasPermission(session, PUBLISHER_PERMISSIONS);
    }

    /**
     * @param email the email of the user to check.
     * @return boolean
     * @throws IOException
     * @deprecated not used
     */
    @Override
    public boolean isPublisher(String email) throws IOException {
        boolean result = true;
        if (StringUtils.isEmpty(email)) {
            warn().user(email)
                    .log("request for isPublisher unsuccessful empty email");
            result = false;
        } else {
            try {
                Session s = getSessionfromEmail(email);
                if (!hasPermission(s, PUBLISHER_PERMISSIONS)) {
                    warn().user(email)
                            .log("request for isPublisher privilages check unsuccessful");
                    result = false;
                }

            } catch (Exception exception) {
                warn().user(email)
                        .log("request for publisher privilages exception caught");
                result = false;
            }
        }
        return result;
    }

    /**
     * @param session {@link Session} to get the user details from.
     * @return boolean if valid session with email and has admin permissions
     * @throws JWTVerificationException
     */
    @Override
    public boolean isAdministrator(Session session) throws JWTVerificationException {
        return session != null && !StringUtils.isEmpty(session.getEmail()) &&
                hasPermission(session, ADMIN_PERMISSIONS);
    }

    /**
     * @param email the email of the user to check.
     * @return
     * @throws JWTVerificationException
     */
    public boolean isAdministrator(String email) throws JWTVerificationException {
        boolean result = true;
        if (StringUtils.isEmpty(email)) {
            warn().user(email)
                    .log("request for isAdministrator unsuccessful empty email");
            result = false;
        } else {
            try {
                Session s = getSessionfromEmail(email);
                if (!hasPermission(s, ADMIN_PERMISSIONS)) {
                    warn().user(email)
                            .log("request for admin privilages check unsuccessful");
                    result = false;
                }

            } catch (Exception exception) {
                warn().user(email)
                        .log("request for admin privilages exception caught");
                result = false;
            }
        }

        return result;
    }


    /**
     * @param collection the collection to check users against.
     * @return
     * @throws JWTVerificationException
     * @deprecated not used
     */

    @Override
    public List<User> getCollectionAccessMapping(Collection collection) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    /**
     * @return
     * @throws JWTVerificationException
     * @deprecated with dp-identity-api
     */
    @Override
    public boolean hasAdministrator() throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    /**
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the user granting the permission.
     * @throws JWTVerificationException
     * @deprecated with dp-identity-api
     */
    @Override
    public void addAdministrator(String email, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    /**
     * @param email   the email of the user to remove the permission from.
     * @param session the {@link Session} of the user revoking the permission.
     * @throws JWTVerificationException
     * @deprecated with dp-identity-api
     */
    @Override
    public void removeAdministrator(String email, Session session) throws JWTVerificationException {
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

    /**
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the {@link User} granting the permissison.
     * @throws JWTVerificationException
     * @deprecated with dp-identity-api
     */
    @Override
    public void addEditor(String email, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    /**
     * @param email   the email of the user to revoke the permission from.
     * @param session the {@link Session} of the {@link User} revoking the permissison.
     * @throws JWTVerificationException
     * @deprecated with dp-identity-api
     */
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

    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} in question.
     * @param team                  the {@link Team} to permit view permission to.
     * @param session               the {@link Session} of the user granting the permission.
     * @throws JWTVerificationException
     * @deprecated with dp-identity-api
     */
    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws JWTVerificationException {
        throw new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR);
    }

    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to remove the team.
     * @param team                  the {@link Team} to remove.
     * @param session               the {@link Session} of the user revoking view permission.
     * @throws JWTVerificationException
     * @deprecated with dp-identity-api
     */
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


    /**
     * @param session
     * @param permission
     * @return
     */
    public boolean hasPermission(Session session, String permission) {
        try {
            return ArrayUtils.contains(session.getGroups(), permission);
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * @param email
     * @return
     */
    public Session getSessionfromEmail(String email) {
        try {
            return sessionsService.find(email);

        } catch (Exception exception) {

            return null;
        }
    }
}