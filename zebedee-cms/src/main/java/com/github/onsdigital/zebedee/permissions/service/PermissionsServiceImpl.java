package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyManager;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_VIEWER_TEAM_ADDED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_VIEWER_TEAM_REMOVED;
import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory.getCollectionHistoryDao;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.teamAdded;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.teamRemoved;

/**
 * Handles permissions mapping between users and {@link com.github.onsdigital.zebedee.Zebedee} functions.
 * Created by david on 12/03/2015.
 */
public class PermissionsServiceImpl implements PermissionsService {

    private PermissionsStore permissionsStore;
    private KeyringCache keyringCache;
    private ReadWriteLock accessMappingLock = new ReentrantReadWriteLock();
    private ServiceSupplier<UsersService> usersServiceSupplier;
    private ServiceSupplier<TeamsService> teamsServiceSupplier;

    /**
     * @param permissionsStore
     * @param usersServiceSupplier
     * @param teamsServiceSupplier
     * @param keyringCache
     */
    public PermissionsServiceImpl(PermissionsStore permissionsStore, ServiceSupplier<UsersService> usersServiceSupplier,
                                  ServiceSupplier<TeamsService> teamsServiceSupplier, KeyringCache keyringCache) {
        this.permissionsStore = permissionsStore;
        this.usersServiceSupplier = usersServiceSupplier;
        this.teamsServiceSupplier = teamsServiceSupplier;
        this.keyringCache = keyringCache;
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

    /**
     * Determines whether the specified user has publisher permissions.
     *
     * @param email The user's email.
     * @return If the user is an publisher, true.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean isPublisher(String email) throws IOException {
        if (StringUtils.isEmpty(email)) {
            return false;
        }
        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        return isPublisher(email, accessMapping);
    }

    private boolean isPublisher(String email, AccessMapping accessMapping) {
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
    @Override
    public boolean isAdministrator(String email) throws IOException {
        if (StringUtils.isEmpty(email)) {
            return false;
        }
        return isAdministrator(email, permissionsStore.getAccessMapping());
    }

    @Override
    public List<User> getCollectionAccessMapping(Collection collection) throws IOException {
        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        List<Team> teamsList = teamsServiceSupplier.getService().listTeams();
        List<User> keyUsers = usersServiceSupplier
                .getService()
                .list()
                .stream()
                .filter(user -> isCollectionKeyRecipient(accessMapping, teamsList, user, collection))
                .collect(Collectors.toList());
        return keyUsers;
    }

    private boolean isCollectionKeyRecipient(AccessMapping accessMapping, List<Team> teamsList, User user, Collection collection) {
        boolean result = false;
        try {
            result = isAdministrator(user.getEmail(), accessMapping)
                    || canEdit(user.getEmail())
                    || canView(user.getEmail(), collection.getDescription(), accessMapping, teamsList);
        } catch (IOException e) {
            logError(e).throwUnchecked(e);
        }
        return result;
    }

    private boolean isAdministrator(String email, AccessMapping accessMapping) {
        return accessMapping.getAdministrators() != null && accessMapping.getAdministrators().contains(standardise(email));
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
    @Override
    public boolean canEdit(String email) throws IOException {
        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        return canEdit(email, accessMapping);
    }

    /**
     * Get whether a user session can edit a collection
     * <p>
     * Future-proofing only at present
     *
     * @param session               the session
     * @param collectionDescription the collection description
     * @return
     * @throws IOException
     */
    @Override
    public boolean canEdit(Session session, CollectionDescription collectionDescription) throws IOException {
        if (collectionDescription.isEncrypted) {
            return canEdit(session.getEmail()) && keyringCache.get(session).list().contains
                    (collectionDescription.getId());
        } else {
            return canEdit(session.getEmail());
        }
    }

    /**
     * Get whether a user can edit a collection
     *
     * @param user                  the user
     * @param collectionDescription
     * @return
     * @throws IOException
     */
    @Override
    public boolean canEdit(User user, CollectionDescription collectionDescription) throws IOException {
        if (collectionDescription.isEncrypted) {
            return canEdit(user.getEmail()) && user.keyring().list().contains(collectionDescription.getId());
        } else {
            return canEdit(user.getEmail());
        }
    }


    /**
     * Get whether a user can edit a collection
     *
     * @param email                 the user email
     * @param collectionDescription
     * @return
     * @throws IOException
     */
    @Override
    public boolean canEdit(String email, CollectionDescription collectionDescription) throws IOException {
        try {
            return canEdit(usersServiceSupplier.getService().getUserByEmail(email), collectionDescription);
        } catch (BadRequestException | NotFoundException e) {
            return false;
        }
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

        // Update keyring (assuming this is not the system initialisation)
        updateKeyring(session, email);
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
     * *
     *
     * @param user                  The user. Can be null.
     * @param collectionDescription The collection to check access for.
     * @return True if the user is a member of the Digital Publishing team or
     * the user is a content owner with access to the given path or any parent path.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean canView(User user, CollectionDescription collectionDescription) throws IOException {
        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        return user != null && (
                canEdit(user.getEmail(), accessMapping) || canView(user.getEmail(), collectionDescription, accessMapping));
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
    @Override
    public boolean canView(String email, CollectionDescription collectionDescription) throws IOException {

        try {
            return canView(usersServiceSupplier.getService().getUserByEmail(email), collectionDescription);
        } catch (NotFoundException | BadRequestException e) {
            return false;
        }
    }

    /**
     * Grants the given team access to the given collection.
     *
     * @param collectionDescription The collection to give the team access to.
     * @param team                  The team to be granted access.
     * @param session               Only editors can grant a team access to a collection.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException {
        if (session == null || !canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        Set<Integer> collectionTeams = accessMapping.getCollections().get(collectionDescription.getId());
        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
            accessMapping.getCollections().put(collectionDescription.getId(), collectionTeams);
        }

        Team teamAdded = !collectionTeams.contains(team.getId()) ? team : null;
        collectionTeams.add(team.getId());
        permissionsStore.saveAccessMapping(accessMapping);

        if (teamAdded != null) {
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
     * @param team                  The team to be revoked access.
     * @param session               Only editors can revoke team access to a collection.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException {
        if (session == null || !canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = permissionsStore.getAccessMapping();
        Set<Integer> collectionTeams = accessMapping.getCollections().get(collectionDescription.getId());
        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
            accessMapping.getCollections().put(collectionDescription.getId(), collectionTeams);
        }


        Team teamRemoved = collectionTeams.contains(team.getId()) ? team : null;
        collectionTeams.remove(team.getId());
        permissionsStore.saveAccessMapping(accessMapping);

        if (teamRemoved != null) {
            getCollectionHistoryDao().saveCollectionHistoryEvent(collectionDescription.getId(), collectionDescription.getName(),
                    session, COLLECTION_VIEWER_TEAM_REMOVED, teamRemoved(collectionDescription, session, team));
        }
    }

    private boolean canEdit(String email, AccessMapping accessMapping) {
        Set<String> digitalPublishingTeam = accessMapping.getDigitalPublishingTeam();
        return (digitalPublishingTeam != null && digitalPublishingTeam.contains(standardise(email)));
    }


    private boolean canView(String email, CollectionDescription collectionDescription, AccessMapping accessMapping)
            throws IOException {

        // Check to see if the email is a member of a team associated with the given collection:
        Set<Integer> teams = accessMapping.getCollections().get(collectionDescription.getId());
        if (teams == null) {
            return false;
        }

        return teamsServiceSupplier.getService()
                .listTeams()
                .stream()
                .filter(team -> teams.contains(team.getId()) && team.getMembers().contains(standardise(email)))
                .findFirst()
                .isPresent();
    }

    private boolean canView(String email, CollectionDescription collectionDescription,
                            AccessMapping accessMapping, List<Team> teamsList) throws IOException {
        Set<Integer> collectionTeams = accessMapping.getCollections().get(collectionDescription.getId());
        if (collectionTeams == null || collectionTeams.isEmpty()) {
            return false;
        }
        return teamsList.stream()
                .filter(t -> collectionTeams.contains(t.getId()) && t.getMembers().contains(standardise(email)))
                .findFirst()
                .isPresent();
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
     * @throws NotFoundException     If the user cannot be found
     * @throws UnauthorizedException If the request is not from an admin or publisher
     */
    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException,
            UnauthorizedException {
        AccessMapping accessMapping = permissionsStore.getAccessMapping();

        if ((session == null) || (!isAdministrator(session.getEmail(), accessMapping) && !isPublisher(session.getEmail(), accessMapping)
                && !session.getEmail().equalsIgnoreCase(email))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        return new PermissionDefinition()
                .setEmail(email)
                .isAdmin(isAdministrator(email))
                .isEditor(canEdit(email));
    }

    /**
     * Add the necessary keyrings to the user.
     *
     * @param session The session of the user who is adding the new user.
     * @param email   the email of the new user.
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    private void updateKeyring(Session session, String email)
            throws IOException, NotFoundException, BadRequestException {
        User user = usersServiceSupplier.getService().getUserByEmail(email);
        if (session != null && user.keyring() != null) {
            KeyManager.transferKeyring(user.keyring(), keyringCache.get(session));
            usersServiceSupplier.getService().updateKeyring(user);
        }
    }
}
