package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.zebedee.exceptions.UnsupportedOperationExceptions;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

public class JWTPermissionsServiceImpl implements PermissionsService {

    static final String PUBLISHER_PERMISSIONS = "role-publisher";
    static final String ADMIN_PERMISSIONS = "role-admin";
    static final String JWTPERMISSIONSSERVICE_ERROR =
            "error accessing JWTPermissions Service";
    private Sessions sessionsService;
    private PermissionsStore permissionsStore;

    private ServiceSupplier<UsersService> usersServiceSupplier;
    private ServiceSupplier<TeamsService> teamsServiceSupplier;

    /**
     * this has been implemented for the migration to using JWT Session
     * to implement permissionstore modules when the jwt is enablede
     * Update Zebedee permissions service to get list of groups for user from JWT session store
     *
     * @param sessionService - {@link Sessions}
     */

    public JWTPermissionsServiceImpl(Sessions sessionService) {
    }

    /**
     * @param session
     * @return
     */
    public static List<Integer> convertGroupsToTeams(Session session) {
        if (session.getGroups() == null || Arrays.stream(session.getGroups()).count() == 0) {
            throw new JWTVerificationException("JWT Permissions service error for convertGroupsToTeams no groups");
        }
        String[] groups = session.getGroups();
        Set<String> setOfString = new HashSet<>(
                Arrays.asList(groups));
        List<Integer> teamsList = new ArrayList<>();
        for (String s : setOfString) {
            try {
                teamsList.add(Integer.parseInt(s));
            } catch (NumberFormatException ex) {
            }
        }

        return teamsList;
    }

    /**
     * if the valid session groups contain the role publisher
     * implemented as part of session migration to JWT
     * Get JWT from JWT session service and check if the user has the 'Publisher' permission in their groups.
     *
     * @param session {@link Session} to get the user details from.
     * @return boolean
     * @throws JWTVerificationException
     */
    @Override
    public boolean isPublisher(Session session) throws IOException {
        return session != null && !StringUtils.isEmpty(session.getEmail()) &&
                hasPermission(session, PUBLISHER_PERMISSIONS);
    }

    /**
     * from the email get the session  and then groups contain the role publisher
     * implemented as part of session migration to JWT
     *
     * @param email the email of the user to check.
     * @return boolean
     * @throws IOException
     */
    @Override
    public boolean isPublisher(String email) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for isPublisher no longer required");
    }


    /**
     * implemented as part of session migration to JWT
     *
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
     * implemented as part of session migration to JWT
     *
     * @param email the email of the user to check.
     * @return
     * @throws JWTVerificationException
     */
    public boolean isAdministrator(String email) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for isAdministrator no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if envoked
     *
     * @param collection the collection to check users against.
     * @return
     * @throws JWTVerificationException
     */

    @Override
    public List<User> getCollectionAccessMapping(Collection collection) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for getCollectionAccessMapping no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if envoked
     *
     * @return
     * @throws JWTVerificationException
     */
    @Override
    public boolean hasAdministrator() throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for hasAdministrator no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if envoked
     *
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the user granting the permission.
     * @throws JWTVerificationException
     */
    @Override
    public void addAdministrator(String email, Session session) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for addAdministrator no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if envoked
     *
     * @param email   the email of the user to remove the permission from.
     * @param session the {@link Session} of the user revoking the permission.
     * @throws IOException
     */
    @Override
    public void removeAdministrator(String email, Session session) throws IOException {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for removeAdministrator no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * previous version flow...
     * canEdit(Session session) -> canEdit(session.getEmail() -> canEdit(email, accessMapping)
     * so  returns whether email is in digitalPublishingTeam and digitalPublishingTeam is not null ...
     * after migration to dp-identity this translates to isPublisher
     *
     * @param session the {@link Session} of the user to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canEdit(Session session) throws IOException {
        return isPublisher(session);
    }

    /**
     * @param email the email of the user to check.
     * @return
     * @throws JWTVerificationException
     */
    @Override
    public boolean canEdit(String email) throws IOException {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canEdit no longer required");
    }

    /**
     * @param user the {@link User} to check.
     * @return
     * @throws JWTVerificationException
     */
    @Override
    public boolean canEdit(User user) throws IOException {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canEdit no longer required");
    }


    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if envoked
     *
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the {@link User} granting the permissison.
     * @throws IOException
     */
    @Override
    public void addEditor(String email, Session session) throws IOException {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for addEditor no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if envoked
     *
     * @param email   the email of the user to revoke the permission from.
     * @param session the {@link Session} of the {@link User} revoking the permissison.
     * @throws UnsupportedOperationExceptions
     */
    @Override
    public void removeEditor(String email, Session session) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for removeEditor no longer required");
    }

    /**
     * Determines whether the subscribed session has viewing authorisation to collection
     *
     * @param session               the {@link Session} to get the user details from.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return
     * @throws IOException
     */
    @Override
    public boolean canView(Session session, CollectionDescription collectionDescription) throws IOException {
        List<Integer> teams = convertGroupsToTeams(session);
        AccessMapping accessMapping = null;
        try {
            accessMapping = permissionsStore.getAccessMapping();
        } catch (IOException e) {
        }

        Set<Integer> collectionTeams = accessMapping.getCollections().get(collectionDescription.getId());
        if (collectionTeams == null || collectionTeams.isEmpty()) {
            return false;
        }
        return teams.stream().anyMatch(t -> collectionTeams.contains(t));
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if envoked
     *
     * @param user                  the {@link User} to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return
     * @throws UnsupportedOperationExceptions
     */
    @Override
    public boolean canView(User user, CollectionDescription collectionDescription) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canView no longer required");
    }

    /**
     * @param email                 the email of the user to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return
     * @throws UnsupportedOperationExceptions
     */
    @Override
    public boolean canView(String email, CollectionDescription collectionDescription) throws
            UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canView no longer required");
    }

    private boolean canView(String email, CollectionDescription collectionDescription, AccessMapping accessMapping) throws
            UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canView no longer required");
    }


    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} in question.
     * @param team                  the {@link Team} to permit view permission to.
     * @param session               the {@link Session} of the user granting the permission.
     * @throws JWTVerificationException
     */
    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws
            UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for CollectionDescription no longer required");
    }

    /**
     * this method is being migrated to the dp-identity-api
     *
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to get the viewer
     *                              teams for.
     * @param session               the {@link Session} of the {@link User} requesting this information.
     * @return
     * @throws UnsupportedOperationExceptions
     */
    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws
            UnsupportedOperationExceptions {
//        throw new UnsupportedOperationExceptions("JWT Permissions service error for listViewerTeams no longer required");
        // TODO: 16/12/2021
        return null;
    }

    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to remove the team.
     * @param team                  the {@link Team} to remove.
     * @param session               the {@link Session} of the user revoking view permission.
     * @throws JWTVerificationException
     * @deprecated with dp-identity-api
     */
    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws
            UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for removeViewerTeam no longer required");
    }

    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for userPermissions no longer required");
    }

    @Override
    public Set<String> listCollectionsAccessibleByTeam(Team t) throws IOException {
        // TODO: 16/12/2021
//        AccessMapping accessMapping = permissionsStore.getAccessMapping();
//        if (accessMapping == null) {
//            throw new IOException("error reading accessMapping expected value but was null");
//        }
//
//        if (accessMapping.getCollections() == null) {
//            return new HashSet<>();
    }


    public boolean hasPermission(Session session, String permission) {
        return ArrayUtils.contains(session.getGroups(), permission);
    }

}