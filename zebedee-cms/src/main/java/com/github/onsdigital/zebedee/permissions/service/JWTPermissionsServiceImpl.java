package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.UnsupportedOperationExceptions;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JWTPermissionsServiceImpl implements PermissionsService {

    static final String PUBLISHER_PERMISSIONS = "role-publisher";
    static final String ADMIN_PERMISSIONS = "role-admin";
    private PermissionsStore permissionsStore;


    /**
     * this has been implemented for the migration to using JWT Session
     * to implement 'PermissionStore' modules when the jwt is enabled
     * Update Zebedee permissions service to get list of groups for user from JWT session store
     *
     * @param sessionService - {@link Sessions}
     */

    public JWTPermissionsServiceImpl(Sessions sessionService) {
    }

    /**
     * @param session - {@link Sessions} to get the groups
     * @return list of groups where groupId is converted to teamId
     * @throws IOException if session has no groups or empty list
     */
    public static List<Integer> convertGroupsToTeams(Session session) throws IOException {
        if (session.getGroups() == null || !Arrays.stream(session.getGroups()).findAny().isPresent()) {
            throw new IOException("JWT Permissions service error for convertGroupsToTeams no groups ");
        }
        String[] groups = session.getGroups();
        Set<String> setOfString = new HashSet<>(
                Arrays.asList(groups));
        List<Integer> teamsList = new ArrayList<>();
        for (String s : setOfString) {
            try {
                teamsList.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
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
     * @throws IOException If a filesystem error occurs.
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
     * @throws UnsupportedOperationExceptions if invoked will error
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
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean isAdministrator(Session session) throws IOException {
        return session != null && !StringUtils.isEmpty(session.getEmail()) &&
                hasPermission(session, ADMIN_PERMISSIONS);
    }

    /**
     * implemented as part of session migration to JWT
     *
     * @param email the email of the user to check.
     * @return null
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public boolean isAdministrator(String email) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for isAdministrator no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if invoked
     *
     * @return null
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public boolean hasAdministrator() throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for hasAdministrator no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if invoked
     *
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the user granting the permission.
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public void addAdministrator(String email, Session session) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for addAdministrator no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if invoked
     *
     * @param email   the email of the user to remove the permission from.
     * @param session the {@link Session} of the user revoking the permission.
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public void removeAdministrator(String email, Session session) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for removeAdministrator no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * previous version flow...
     * canEdit(session) -> canEdit(session.getEmail()) -> canEdit(email, accessMapping)
     * so  returns whether email is in digitalPublishingTeam and digitalPublishingTeam is not null ...
     * after migration to dp-identity this translates to isPublisher
     *
     * @param session the {@link Session} of the user to check.
     * @return null
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean canEdit(Session session) throws IOException {
        return session != null && !StringUtils.isEmpty(session.getEmail()) &&
                (hasPermission(session, PUBLISHER_PERMISSIONS) || hasPermission(session, ADMIN_PERMISSIONS));

    }

    /**
     * @param email the email of the user to check.
     * @return null
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public boolean canEdit(String email) throws IOException {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canEdit no longer required");
    }

    /**
     * @param user the {@link User} to check.
     * @return null
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public boolean canEdit(User user) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canEdit no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if invoked
     *
     * @param email   the email of the user to permit the permission to.
     * @param session the {@link Session} of the {@link User} granting the permission.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void addEditor(String email, Session session) throws IOException {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for addEditor no longer required");
    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if invoked
     *
     * @param email   the email of the user to revoke the permission from.
     * @param session the {@link Session} of the {@link User} revoking the permission.
     * @throws UnsupportedOperationExceptions will error if invoked.
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
     * @return null
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean canView(Session session, CollectionDescription collectionDescription) throws IOException {
        List<Integer> teams = convertGroupsToTeams(session);
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            Set<Integer> collectionTeams = accessMapping.getCollections().get(collectionDescription.getId());
            if (collectionTeams == null || collectionTeams.isEmpty()) {
                return false;
            }
            return teams.stream().anyMatch(t -> collectionTeams.contains(t));
        } catch (IOException ignored) {
            return false;
        }

    }

    /**
     * implemented as part of session migration to JWT
     * will not be required once jwt has been migrated but will error if invoked
     *
     * @param user                  the {@link User} to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return null
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public boolean canView(User user, CollectionDescription collectionDescription) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canView no longer required");
    }

    /**
     * @param email                 the email of the user to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return null
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public boolean canView(String email, CollectionDescription collectionDescription) throws
            UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for canView no longer required");

    }

    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} in question.
     * @param teamId                the {@link Team} to permit view permission to.
     * @param session               the {@link Session} of the user granting the permission.
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Integer teamId, Session session) throws
            UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for CollectionDescription no longer required");
    }

    /**
     * this method is being migrated to the dp-identity-api
     *
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to get the viewer
     *                              teams for.
     * @param session               the {@link Session} of the {@link User} requesting this information.
     * @return null
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws
            UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for listViewerTeams no longer required");

    }

    /**
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to remove the team.
     * @param teamId                the {@link Team} to remove.
     * @param session               the {@link Session} of the user revoking view permission.
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Integer teamId, Session session) throws
            UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for removeViewerTeam no longer required");
    }


    /**
     * @param email   the email of the user to get the {@link PermissionDefinition} for.
     * @param session the {@link Session} of the user requesting the {@link PermissionDefinition}.
     * @return null
     * @throws UnsupportedOperationExceptions will error if invoked.
     */
    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws UnsupportedOperationExceptions {
        throw new UnsupportedOperationExceptions("JWT Permissions service error for userPermissions no longer required");
    }

    /**
     * @param t the team to check.
     * @return list of teams/groups
     * @throws IOException If a filesystem error occurs
     */
    @Override
    public Set<String> listCollectionsAccessibleByTeam(Team t) throws IOException {
        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        if (accessMapping == null) {
            throw new IOException("error reading accessMapping expected value but was null");
        }

        if (accessMapping.getCollections() == null) {
            return new HashSet<>();
        }

        // AccessMapping.Collections maps CollectionID -> List of Team IDs.
        // Filter to find all the collections who have the specified team ID assigned to them.
        // Returns a Set of collectionIDs matching given criteria.
        return accessMapping.getCollections()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().contains(t.getId()))
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet());
    }


    /**
     * @param session    the {@link Session} to check
     * @param permission the role to check
     * @return boolean
     */
    public boolean hasPermission(Session session, String permission) {
        return ArrayUtils.contains(session.getGroups(), permission);
    }
}