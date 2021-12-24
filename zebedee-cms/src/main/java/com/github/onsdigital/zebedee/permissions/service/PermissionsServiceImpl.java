package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_VIEWER_TEAM_ADDED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_VIEWER_TEAM_REMOVED;
import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory.getCollectionHistoryDao;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.teamAdded;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.teamRemoved;

/**
 * @deprecated this implementation is deprecated and will be removed once the JWT session migration has been completed
 *
 * // TODO: remove this class once the migration to JWT sessions has been completed
 */
@Deprecated
public class PermissionsServiceImpl implements PermissionsService {

    private PermissionsStore permissionsStore;
    private ReadWriteLock accessMappingLock = new ReentrantReadWriteLock();
    private ServiceSupplier<UsersService> usersServiceSupplier;
    private ServiceSupplier<TeamsService> teamsServiceSupplier;

    /**
     * @param permissionsStore
     * @param usersServiceSupplier
     * @param teamsServiceSupplier
     */
    public PermissionsServiceImpl(PermissionsStore permissionsStore, ServiceSupplier<UsersService> usersServiceSupplier,
                                  ServiceSupplier<TeamsService> teamsServiceSupplier) {
        this.permissionsStore = permissionsStore;
        this.usersServiceSupplier = usersServiceSupplier;
        this.teamsServiceSupplier = teamsServiceSupplier;
    }

    /**
     * Determines whether the specified user has publisher permissions/
     *
     * @param session The user's login session.
     * @return If the user is a publisher, true.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean isPublisher(Session session) throws IOException {
        if (session == null || StringUtils.isEmpty(session.getEmail())) {
            return false;
        }
        return isPublisher(session.getEmail());
    }

    private boolean isPublisher(String email) throws IOException {
        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        return accessMapping.getDigitalPublishingTeam() != null && accessMapping.getDigitalPublishingTeam()
                .contains(standardise(email));
    }

    /**
     * Determines whether the specified user has administator permissions.
     *
     * @param session The user's login session.
     * @return If the user is an administrator, true.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean isAdministrator(Session session) throws IOException {
        if (session == null || StringUtils.isEmpty(session.getEmail()))
            return false;
        return isAdministrator(session.getEmail());
    }

    /**
     * Determines whether the specified user has administator permissions.
     *
     * @param email The user's emal.
     * @return If the user is an administrator, true.
     * @throws IOException If a filesystem error occurs.
     */
    private boolean isAdministrator(String email) throws IOException {
        if (StringUtils.isEmpty(email)) {
            return false;
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();

        return accessMapping.getAdministrators() != null && accessMapping.getAdministrators()
                .contains(standardise(email));
    }

    /**
     * Determines whether an administator exists.
     *
     * @return True if at least one administrator exists.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean hasAdministrator() throws IOException {
        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        return accessMapping.getAdministrators() != null && !accessMapping.getAdministrators().isEmpty();
    }

    /**
     * Adds the specified user to the administrators, giving them administrator permissions (but not content permissions).
     * <p/>
     * <p>If no administrator exists the first call will succeed otherwise </p>
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        // Allow the initial user to be set as an administrator:
        if (hasAdministrator() && (session == null || StringUtils.isEmpty(session.getEmail())
                || !isAdministrator(session.getEmail()))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }


        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        if (accessMapping.getAdministrators() == null) {
            accessMapping.setAdministrators(new HashSet<>());
        }
        accessMapping.getAdministrators().add(standardise(email));
        permissionsStore.saveAccessMapping(accessMapping);
    }

    /**
     * Removes the specified user from the administrators, revoking administrative permissions (but not content permissions).
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (session == null || StringUtils.isEmpty(email) || StringUtils.isEmpty(session.getEmail()) || !isAdministrator(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        if (accessMapping.getAdministrators() == null) {
            accessMapping.setAdministrators(new HashSet<>());
        }
        accessMapping.getAdministrators().remove(standardise(email));
        permissionsStore.saveAccessMapping(accessMapping);
    }

    /**
     * Determines whether the specified user has editing rights.
     *
     * @param session The user's session - this may be null.
     * @return If the user is a member of the Digital Publishing team, true.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean canEdit(Session session) throws IOException {
        return session != null && canEdit(session.getEmail());
    }

    /**
     * Responds only on the basis of whether a user is an editor
     *
     * @param email The user's email.
     * @return If the user is a member of the Digital Publishing team, true.
     * @throws IOException If a filesystem error occurs.
     */
    private boolean canEdit(String email) throws IOException {
        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        return canEdit(email, accessMapping);
    }

    /**
     * Adds the specified user to the Digital Publishing team, giving them access to read and write all content.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        if (hasAdministrator() && (session == null || !isAdministrator(session.getEmail()))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        accessMapping.getDigitalPublishingTeam().add(PathUtils.standardise(email));
        permissionsStore.saveAccessMapping(accessMapping);
    }


    /**
     * Removes the specified user to the Digital Publishing team, revoking access to read and write all content.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        if (session == null || StringUtils.isEmpty(email) || StringUtils.isEmpty(session.getEmail())
                || !isAdministrator(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        //if (accessMapping.digitalPublishingTeam == null) {
        //    accessMapping.digitalPublishingTeam = new HashSet<>();
        //}
        accessMapping.getDigitalPublishingTeam().remove(PathUtils.standardise(email));
        permissionsStore.saveAccessMapping(accessMapping);
    }

    /**
     * Determines whether a session has viewing rights.
     *
     * @param session               The user's session. Can be null.
     * @param collectionDescription The collection to check access for.
     * @return True if the user is a member of the Digital Publishing team or
     * the user is a content owner with access to the given path or any parent path.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean canView(Session session, CollectionDescription collectionDescription) throws IOException {
        return session != null && canView(session.getEmail(), collectionDescription);
    }

    /**
     * Determines whether the specified user has viewing rights.
     *
     * @param email                 The email of the user
     * @param collectionDescription The collection to check access for.
     * @return True if the user is a member of the Digital Publishing team or
     * the user is a content owner with access to the given path or any parent path.
     * @throws IOException If a filesystem error occurs.
     */
    private boolean canView(String email, CollectionDescription collectionDescription) throws IOException {
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            return canEdit(email, accessMapping) || canView(email, collectionDescription, accessMapping);
        } catch (IOException e) {
            error().data("collectionId", collectionDescription.getId()).data("user", email)
                    .logException(e, "canView permission request denied: unexpected error");
        }
        return false;
    }

    /**
     * Grant view permissions to a team.
     *
     * @param collectionDescription The {@link CollectionDescription} of the collection to give the team access to.
     * @param teamId                the ID of the team to permit view permission to.
     * @param session               the {@link Session} of the user granting the permission. Only editors can permit a team access to a collection.
     * @throws IOException If a filesystem error occurs.
     * @throws ZebedeeException if the user is not authorised to add view team permissions.
     */
    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Integer teamId, Session session) throws IOException, ZebedeeException {
        if (session == null || !canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        Set<Integer> collectionTeams = accessMapping.getCollections().get(collectionDescription.getId());
        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
            accessMapping.getCollections().put(collectionDescription.getId(), collectionTeams);
        }


        if (!collectionTeams.contains(teamId)) {
            collectionTeams.add(teamId);
            permissionsStore.saveAccessMapping(accessMapping);

            Team team = new Team();
            team.setId(teamId);
            getCollectionHistoryDao().saveCollectionHistoryEvent(collectionDescription.getId(), collectionDescription
                    .getName(), session, COLLECTION_VIEWER_TEAM_ADDED, teamAdded(collectionDescription, session, team));
        }
    }

    /**
     * Provide a list of team ID's currently associated with a collection
     *
     * @param collectionDescription
     * @param session
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws IOException, UnauthorizedException {
        if (session == null || !canView(session, collectionDescription)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        Set<Integer> teamIds = accessMapping.getCollections().get(collectionDescription.getId());
        if (teamIds == null) teamIds = new HashSet<>();

        return java.util.Collections.unmodifiableSet(teamIds);
    }

    /**
     * Revokes access for given team to the given collection.
     *
     * @param collectionDescription The collection to revoke team access to.
     * @param teamId                The id of the team to be revoked access.
     * @param session               Only editors can revoke team access to a collection.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Integer teamId, Session session) throws IOException, ZebedeeException {
        if (session == null || !canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        Set<Integer> collectionTeams = accessMapping.getCollections().get(collectionDescription.getId());
        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
            accessMapping.getCollections().put(collectionDescription.getId(), collectionTeams);
        }

        if (collectionTeams.contains(teamId)) {
            collectionTeams.remove(teamId);
            permissionsStore.saveAccessMapping(accessMapping);
            getCollectionHistoryDao().saveCollectionHistoryEvent(collectionDescription.getId(), collectionDescription.getName(),
                    session, COLLECTION_VIEWER_TEAM_REMOVED, teamRemoved(collectionDescription, session, teamId));
        }
    }

    private boolean canEdit(String email, AccessMapping accessMapping) {
        Set<String> digitalPublishingTeam = accessMapping.getDigitalPublishingTeam();
        return (digitalPublishingTeam != null && digitalPublishingTeam.contains(standardise(email)));
    }

    /**
     * @deprecated this method is deprecated and needs to be reimplemented to use the JWT session to determine the
     *             groups/teams a user is a member of.
     *
     * TODO: Add an implementation of this method that will use the groups stored in the JWT session rather than using
     *       the Teams service
     */
    @Deprecated
    private boolean canView(String email, CollectionDescription collectionDescription, AccessMapping accessMapping)
            throws IOException {

        // Check to see if the email is a member of a team associated with the given collection:
        Set<Integer> teamIds = accessMapping.getCollections().get(collectionDescription.getId());
        if (teamIds == null) {
            return false;
        }

        return teamsServiceSupplier.getService().resolveTeams(teamIds).stream()
                .anyMatch(team -> team.getMembers().contains(standardise(email)));
    }

    private String standardise(String email) {
        return PathUtils.standardise(email);
    }

    /**
     * User permission levels given an email
     *
     * @param email the user email
     * @return a {@link PermissionDefinition} object
     * @throws IOException
     * @throws UnauthorizedException If the request is not from an admin or publisher
     */
    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws IOException,
            UnauthorizedException {
        if ((session == null) || (!isAdministrator(session.getEmail()) && !isPublisher(session.getEmail())
                && !session.getEmail().equalsIgnoreCase(email))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        return new PermissionDefinition()
                .setEmail(email)
                .isAdmin(isAdministrator(email))
                .isEditor(canEdit(email));
    }
}
