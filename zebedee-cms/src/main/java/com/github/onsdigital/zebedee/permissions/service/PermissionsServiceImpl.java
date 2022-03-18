package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;

/**
 * @deprecated this implementation is deprecated and will be removed once the JWT session migration has been completed
 *
 * // TODO: remove this class once the migration to JWT sessions has been completed
 */
@Deprecated
public class PermissionsServiceImpl implements PermissionsService {

    private PermissionsStore permissionsStore;
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private ServiceSupplier<TeamsService> teamsServiceSupplier;

    /**
     * @param permissionsStore
     * @param teamsServiceSupplier
     */
    public PermissionsServiceImpl(PermissionsStore permissionsStore,
                                  ServiceSupplier<TeamsService> teamsServiceSupplier) {
        this.permissionsStore = permissionsStore;
        this.teamsServiceSupplier = teamsServiceSupplier;
    }

    /**
     * Determines whether the specified user has publisher permissions
     *
     * @param session The user's login {@link Session}.
     * @return <code>true</code> the user is a publisher or <code>false</code> otherwise.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean isPublisher(Session session) throws IOException {
        if (session == null || StringUtils.isEmpty(session.getEmail())) {
            return false;
        }

        boolean result = false;
        readLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();

            result = accessMapping.getDigitalPublishingTeam() != null && accessMapping.getDigitalPublishingTeam()
                    .contains(standardise(session.getEmail()));
        } finally {
            readLock.unlock();
        }

        return result;
    }

    /**
     * Determines whether the specified user has administator permissions.
     *
     * @param session The user's login {@link Session}.
     * @return <code>true</code> the user is an administrator or <code>false</code> otherwise.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean isAdministrator(Session session) throws IOException {
        if (session == null || StringUtils.isEmpty(session.getEmail())) {
            return false;
        }

        boolean result = false;
        readLock.lock();
        try {

            AccessMapping accessMapping = permissionsStore.getAccessMapping();

            result = accessMapping.getAdministrators() != null && accessMapping.getAdministrators()
                    .contains(standardise(session.getEmail()));
        } finally {
            readLock.unlock();
        }

        return result;
    }

    /**
     * Determines whether an administator exists.
     *
     * @return True if at least one administrator exists.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean hasAdministrator() throws IOException {
        boolean result = false;
        readLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            result = accessMapping.getAdministrators() != null && !accessMapping.getAdministrators().isEmpty();
        } finally {
            readLock.unlock();
        }

        return result;
    }

    /**
     * Adds the specified user to the list of administrators, giving them administrator permissions.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        // Allow the initial user to be set as an administrator:
        if (hasAdministrator() && (session == null || StringUtils.isEmpty(session.getEmail())
                || !isAdministrator(session))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        writeLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            if (accessMapping.getAdministrators() == null) {
                accessMapping.setAdministrators(new HashSet<>());
            }
            accessMapping.getAdministrators().add(standardise(email));
            permissionsStore.saveAccessMapping(accessMapping);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Removes the specified user from the administrators, revoking administrative permissions (but not content permissions).
     *
     * @param email The user's email.
     * @param session the {@link Session} of the user revoking the permission.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (session == null || StringUtils.isEmpty(email) || StringUtils.isEmpty(session.getEmail()) || !isAdministrator(session)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        writeLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            if (accessMapping.getAdministrators() == null) {
                accessMapping.setAdministrators(new HashSet<>());
            }
            accessMapping.getAdministrators().remove(standardise(email));
            permissionsStore.saveAccessMapping(accessMapping);
        } finally {
            writeLock.unlock();
        }
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
        if (session == null || StringUtils.isEmpty(session.getEmail())) {
            return false;
        }

        boolean result = false;
        readLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            result = canEdit(session.getEmail(), accessMapping);
        } finally {
            readLock.unlock();
        }

        return result;
    }

    /**
     * Adds the specified user to the Digital Publishing team, giving them access to read and write all content.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        if (hasAdministrator() && (session == null || !isAdministrator(session))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        writeLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            accessMapping.getDigitalPublishingTeam().add(PathUtils.standardise(email));
            permissionsStore.saveAccessMapping(accessMapping);
        } finally {
            writeLock.unlock();
        }
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
                || !isAdministrator(session)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        writeLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            accessMapping.getDigitalPublishingTeam().remove(PathUtils.standardise(email));
            permissionsStore.saveAccessMapping(accessMapping);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Determines whether a session has viewing rights.
     *
     * @param session      The user's session. Can be null.
     * @param collectionId The ID of the collection to check access for.
     * @return True if the user is a member of the Digital Publishing team or
     * the user is a content owner with access to the given path or any parent path.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public boolean canView(Session session, String collectionId) throws IOException {
        if (session == null) {
            return false;
        }

        boolean result = false;
        readLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            result = canEdit(session.getEmail(), accessMapping) || canView(session.getEmail(), collectionId, accessMapping);
        } catch (IOException e) {
            error().data("collectionId", collectionId).data("user", session.getEmail())
                    .logException(e, "canView permission request denied: unexpected error");
        } finally {
            readLock.unlock();
        }

        return result;
    }

    /**
     * Provide a list of team ID's currently associated with a collection
     *
     * @param collectionId the ID of the collection for which to list the viewer teams
     * @param session      the {@link Session} of the user requesting the list of viewer teams
     * @return The list of
     * @throws IOException
     * @throws UnauthorizedException
     */
    @Override
    public Set<String> listViewerTeams(Session session, String collectionId) throws IOException, UnauthorizedException {
        if (session == null || !canView(session, collectionId)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        readLock.lock();
        Set<String> teamIds;
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            teamIds = accessMapping.getCollections().get(collectionId);

            if (teamIds == null) {
                teamIds = new HashSet<>();
            }
        } finally {
            readLock.unlock();
        }

        return java.util.Collections.unmodifiableSet(teamIds);
    }

    /**
     * Set the list of team IDs that are allowed viewer access to a collection
     *
     * @param collectionID    the ID of the collection collection to set viewer permissions for.
     * @param collectionTeams the set of team IDs for which viewer permissions should be granted to the collection.
     * @param session         the session of the user that is attempting to set the viewer permissions.
     * @throws IOException if reading or writing the access mapping fails.
     * @throws UnauthorizedException if the users' session isn't authorised to edit collections.
     */
    @Override
    public void setViewerTeams(Session session, String collectionID, Set<String> collectionTeams) throws IOException, UnauthorizedException {
        if (session == null || !canEdit(session)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
        }

        writeLock.lock();
        try {
            AccessMapping accessMapping = permissionsStore.getAccessMapping();
            accessMapping.getCollections().put(collectionID, collectionTeams);
            permissionsStore.saveAccessMapping(accessMapping);
        } finally {
            writeLock.unlock();
        }
    }

    private boolean canEdit(String email, AccessMapping accessMapping) {
        Set<String> digitalPublishingTeam = accessMapping.getDigitalPublishingTeam();
        return (digitalPublishingTeam != null && digitalPublishingTeam.contains(standardise(email)));
    }

    private boolean canView(String email, String collectionId, AccessMapping accessMapping)
            throws IOException {

        // Check to see if the email is a member of a team associated with the given collection:
        Set<String> teamIds;
        readLock.lock();
        try {
            teamIds = accessMapping.getCollections().get(collectionId);
        } finally {
            readLock.unlock();
        }
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
     * @throws IOException If a filesystem error occurs.
     * @throws UnauthorizedException If the request is not from an admin or publisher
     */
    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws IOException,
            UnauthorizedException {
        if ((session == null) || (!isAdministrator(session) && !isPublisher(session)
                && !session.getEmail().equalsIgnoreCase(email))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // This is a little ugly, but this class is deprecated and will be removed very soon
        Session userSession = new Session(null, email);

        return new PermissionDefinition()
                .setEmail(email)
                .isAdmin(isAdministrator(userSession))
                .isEditor(canEdit(userSession));
    }

    /**
     * Get user permission levels given an session
     *
     * @param session the user session
     * @return a {@link PermissionDefinition} object representing the user's permissions
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public PermissionDefinition userPermissions(Session session) throws IOException {
        return new PermissionDefinition()
                .setEmail(session.getEmail())
                .isAdmin(isAdministrator(session))
                .isEditor(canEdit(session));
    }
}