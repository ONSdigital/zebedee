package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.AccessMapping;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.service.UsersService;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.model.CollectionOwner.DATA_VISUALISATION;
import static com.github.onsdigital.zebedee.model.CollectionOwner.PUBLISHING_SUPPORT;
import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory.getCollectionHistoryDao;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.teamAdded;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.teamRemoved;

/**
 * Handles permissions mapping between users and {@link com.github.onsdigital.zebedee.Zebedee} functions.
 * Created by david on 12/03/2015.
 */
public class Permissions {

    private Zebedee zebedee;
    private Path accessMappingPath;
    private ReadWriteLock accessMappingLock = new ReentrantReadWriteLock();
    /**
     * Wrap static method calls to obtain service in function makes testing easier - class member can be
     * replaced with a mocked giving control of desired behaviour.
     */
    private ServiceSupplier<UsersService> usersServiceSupplier = () -> Root.zebedee.getUsersService();

    public Permissions(Path permissions, Zebedee zebedee) {
        this.zebedee = zebedee;
        accessMappingPath = permissions.resolve("accessMapping.json");
    }

    /**
     * Determines whether the specified user has publisher permissions/
     *
     * @param session The user's login session.
     * @return If the user is a publisher, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean isPublisher(Session session) throws IOException {
        return session != null && isPublisher(session.email);
    }

    /**
     * Determines whether the specified user has publisher permissions.
     *
     * @param email The user's email.
     * @return If the user is an publisher, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean isPublisher(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return isPublisher(email, accessMapping);
    }

    private boolean isPublisher(String email, AccessMapping accessMapping) {
        return accessMapping.digitalPublishingTeam != null && accessMapping.digitalPublishingTeam.contains(standardise(email));
    }

    /**
     * Determines whether the specified user has administator permissions.
     *
     * @param session The user's login session.
     * @return If the user is an administrator, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean isAdministrator(Session session) throws IOException {
        return session != null && isAdministrator(session.email);
    }

    /**
     * Determines whether the specified user has administator permissions.
     *
     * @param email The user's emal.
     * @return If the user is an administrator, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean isAdministrator(String email) throws IOException {
        return isAdministrator(email, readAccessMapping());
    }

    public List<User> getCollectionAccessMapping(Zebedee zebedee, Collection collection) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        List<Team> teamsList = zebedee.getTeams().listTeams();
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
            result = isAdministrator(user.email, accessMapping)
                    || canEdit(user.email)
                    || canView(user.email, collection.getDescription(), accessMapping, teamsList);
        } catch (IOException e) {
            logError(e).throwUnchecked(e);
        }
        return result;
    }

    public boolean isDataVisPublisher(String email, AccessMapping accessMapping) throws IOException {
        return accessMapping.dataVisualisationPublishers != null
                && accessMapping.dataVisualisationPublishers.contains(standardise(email));
    }

    private boolean isAdministrator(String email, AccessMapping accessMapping) {
        return accessMapping.administrators != null && accessMapping.administrators.contains(standardise(email));
    }

    /**
     * Determines whether an administator exists.
     *
     * @return True if at least one administrator exists.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean hasAdministrator() throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return accessMapping.administrators != null && (accessMapping.administrators.size() > 0);
    }

    /**
     * Adds the specified user to the administrators, giving them administrator permissions (but not content permissions).
     * <p/>
     * <p>If no administrator exists the first call will succeed otherwise </p>
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {

        // Allow the initial user to be set as an administrator:
        if (hasAdministrator() && (session == null || !isAdministrator(session.email))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.administrators == null) {
            accessMapping.administrators = new HashSet<>();
        }
        accessMapping.administrators.add(standardise(email));
        writeAccessMapping(accessMapping);
    }

    /**
     * Removes the specified user from the administrators, revoking administrative permissions (but not content permissions).
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.administrators == null) {
            accessMapping.administrators = new HashSet<>();
        }
        accessMapping.administrators.remove(standardise(email));
        writeAccessMapping(accessMapping);
    }

    /**
     * Determines whether the specified user has editing rights.
     *
     * @param session The user's session - this may be null.
     * @return If the user is a member of the Digital Publishing team, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean canEdit(Session session) throws IOException {
        return session != null && canEdit(session.email);
    }

    /**
     * Responds only on the basis of whether a user is an editor
     *
     * @param email The user's email.
     * @return If the user is a member of the Digital Publishing team, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean canEdit(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
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
    public boolean canEdit(Session session, CollectionDescription collectionDescription) throws IOException {
        if (collectionDescription.isEncrypted) {
            return canEdit(session.email) && zebedee.getKeyringCache().get(session).list().contains(collectionDescription.id);
        } else {
            return canEdit(session.email);
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
    public boolean canEdit(User user, CollectionDescription collectionDescription) throws IOException {
        if (collectionDescription.isEncrypted) {
            return canEdit(user.email) && user.keyring.list().contains(collectionDescription.id);
        } else {
            return canEdit(user.email);
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
    public void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        if (hasAdministrator() && (session == null || !isAdministrator(session.email))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        //if (accessMapping.digitalPublishingTeam == null) {
        //    accessMapping.digitalPublishingTeam = new HashSet<>();
        //}
        accessMapping.digitalPublishingTeam.add(PathUtils.standardise(email));
        writeAccessMapping(accessMapping);

        // Update keyring (assuming this is not the system initialisation)
        updateKeyring(session, email, PUBLISHING_SUPPORT);
    }


    /**
     * Removes the specified user to the Digital Publishing team, revoking access to read and write all content.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        //if (accessMapping.digitalPublishingTeam == null) {
        //    accessMapping.digitalPublishingTeam = new HashSet<>();
        //}
        accessMapping.digitalPublishingTeam.remove(PathUtils.standardise(email));
        writeAccessMapping(accessMapping);
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
    public boolean canView(Session session, CollectionDescription collectionDescription) throws IOException {
        return session != null && canView(session.email, collectionDescription);
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
    public boolean canView(User user, CollectionDescription collectionDescription) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return user != null && (
                canEdit(user.email, accessMapping) || canView(user.email, collectionDescription, accessMapping));
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
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException {
        if (session == null || !canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        Set<Integer> collectionTeams = accessMapping.collections.get(collectionDescription.id);
        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
            accessMapping.collections.put(collectionDescription.id, collectionTeams);
        }

        Team teamAdded = !collectionTeams.contains(team.getId()) ? team : null;
        collectionTeams.add(team.getId());
        writeAccessMapping(accessMapping);

        if (teamAdded != null) {
            getCollectionHistoryDao().saveCollectionHistoryEvent(collectionDescription.id, collectionDescription.name, session,
                    CollectionEventType.COLLECTION_VIEWER_TEAM_ADDED, teamAdded(collectionDescription, session, team));
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
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws IOException, UnauthorizedException {
        if (session == null || !canView(session, collectionDescription)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        Set<Integer> teamIds = accessMapping.collections.get(collectionDescription.id);
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
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException {
        if (session == null || !canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        Set<Integer> collectionTeams = accessMapping.collections.get(collectionDescription.id);
        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
            accessMapping.collections.put(collectionDescription.id, collectionTeams);
        }


        Team teamRemoved = collectionTeams.contains(team.getId()) ? team : null;
        collectionTeams.remove(team.getId());
        writeAccessMapping(accessMapping);

        if (teamRemoved != null) {
            getCollectionHistoryDao().saveCollectionHistoryEvent(collectionDescription.id, collectionDescription.name,
                    session, CollectionEventType.COLLECTION_VIEWER_TEAM_REMOVED,
                    teamRemoved(collectionDescription, session, team));
        }
    }

    private boolean canEdit(String email, AccessMapping accessMapping) {
        Set<String> digitalPublishingTeam = accessMapping.digitalPublishingTeam;
        Set<String> dataVisualisationPublishers = accessMapping.dataVisualisationPublishers;

        return (digitalPublishingTeam != null && digitalPublishingTeam.contains(standardise(email)))
                || (dataVisualisationPublishers != null && dataVisualisationPublishers.contains(standardise(email)));
    }


    private boolean canView(String email, CollectionDescription collectionDescription, AccessMapping accessMapping)
            throws IOException {
        boolean result = false;

        // Check to see if the email is a member of a team associated with the given collection:
        Set<Integer> teams = accessMapping.collections.get(collectionDescription.id);
        if (teams != null) {
            for (Team team : zebedee.getTeams().listTeams()) {
                boolean isTeamMember = teams.contains(team.getId()) && team.getMembers().contains(standardise(email));
                boolean inCollectionGroup = getUserCollectionGroup(email, accessMapping).equals(collectionDescription.collectionOwner);
                if (isTeamMember && inCollectionGroup) {
                    return true;
                }
            }
        }
        return result;
    }

    private boolean canView(String email, CollectionDescription collectionDescription,
                            AccessMapping accessMapping, List<Team> teamsList) throws IOException {
        boolean result = false;
        // Check to see if the email is a member of a team associated with the given collection:
        Set<Integer> teamsOnCollection = accessMapping.collections.get(collectionDescription.id);
        if (teamsOnCollection != null) {
            for (Team team : teamsList) {
                boolean isTeamMember = teamsOnCollection.contains(team.getId()) && team.getMembers()
                        .contains(standardise(email));
                boolean inCollectionGroup = getUserCollectionGroup(email, accessMapping).equals(collectionDescription.collectionOwner);
                if (isTeamMember && inCollectionGroup) {
                    return true;
                }
            }
        }
        return result;
    }

    private AccessMapping readAccessMapping() throws IOException {
        AccessMapping result = null;

        if (Files.exists(accessMappingPath)) {

            // Read the configuration
            accessMappingLock.readLock().lock();
            try (InputStream input = Files.newInputStream(accessMappingPath)) {
                result = Serialiser.deserialise(input, AccessMapping.class);
            } finally {
                accessMappingLock.readLock().unlock();
            }

            // Initialise any missing objects:
            if (result.administrators == null) {
                result.administrators = new HashSet<>();
            }
            if (result.digitalPublishingTeam == null) {
                result.digitalPublishingTeam = new HashSet<>();
            }
            if (result.collections == null) {
                result.collections = new HashMap<>();
            }
            if (result.dataVisualisationPublishers == null) {
                result.dataVisualisationPublishers = new HashSet<>();
            }

        } else {

            // Or generate a new one:
            result = new AccessMapping();
            result.administrators = new HashSet<>();
            result.digitalPublishingTeam = new HashSet<>();
            result.collections = new HashMap<>();
            result.dataVisualisationPublishers = new HashSet<>();
            writeAccessMapping(result);
        }

        return result;
    }

    private void writeAccessMapping(AccessMapping accessMapping) throws IOException {

        accessMappingLock.writeLock().lock();
        try (OutputStream output = Files.newOutputStream(accessMappingPath)) {
            Serialiser.serialise(output, accessMapping);
        } finally {
            accessMappingLock.writeLock().unlock();
        }
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
    public PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException, UnauthorizedException {
        AccessMapping accessMapping = readAccessMapping();

        if ((session == null) || (!isAdministrator(session.email, accessMapping) && !isPublisher(session.email, accessMapping)
                && !session.email.equalsIgnoreCase(email))) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        PermissionDefinition definition = new PermissionDefinition();
        definition.email = email;
        definition.admin = isAdministrator(email);
        definition.editor = canEdit(email);
        definition.dataVisPublisher = isDataVisPublisher(email, accessMapping);
        return definition;
    }

    /**
     * Add a Data Visualisation Publisher.
     *
     * @param email
     * @param session
     * @throws ZebedeeException
     */
    public void addDataVisualisationPublisher(String email, Session session) throws ZebedeeException {
        try {
            // Allow the initial user to be set as an administrator:
            if (hasAdministrator() && (session == null || !isAdministrator(session.email))) {
                throw new UnauthorizedException(getUnauthorizedMessage(session));
            }

            AccessMapping accessMapping = readAccessMapping();
            if (accessMapping.dataVisualisationPublishers == null) {
                accessMapping.dataVisualisationPublishers = new HashSet<>();
            }
            accessMapping.dataVisualisationPublishers.add(standardise(email));
            writeAccessMapping(accessMapping);

            // Update keyring (assuming this is not the system initialisation)
            updateKeyring(session, email, DATA_VISUALISATION);
        } catch (IOException e) {
            logError(e, "Error while attempting to add Data Vis publisher permission.")
                    .user(session.email)
                    .addParameter("forUser", email)
                    .log();
            throw new UnexpectedErrorException("Error while attempting to add Data Vis publisher permission.",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    public void removeDataVisualisationPublisher(String email, Session session) throws IOException, UnauthorizedException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        accessMapping.dataVisualisationPublishers.remove(PathUtils.standardise(email));
        writeAccessMapping(accessMapping);
    }

    /**
     * Determined the {@link CollectionOwner} for this collection (PUBLISHING_SUPPORT or Data Visualisation).
     */
    public CollectionOwner getUserCollectionGroup(Session session) throws IOException {
        return getUserCollectionGroup(session.email, readAccessMapping());
    }

    public CollectionOwner getUserCollectionGroup(String email) throws IOException {
        return getUserCollectionGroup(email, readAccessMapping());
    }

    public CollectionOwner getUserCollectionGroup(String email, AccessMapping accessMapping) throws IOException {
        return isDataVisPublisher(email, accessMapping) ? DATA_VISUALISATION : PUBLISHING_SUPPORT;
    }

    /**
     * Add the necessary keyrings to the user.
     *
     * @param session         The session of the user who is adding the new user.
     * @param email           the email of the new user.
     * @param collectionOwner {@link CollectionOwner#PUBLISHING_SUPPORT} for PST users
     *                        or {@link CollectionOwner#DATA_VISUALISATION} for data vis users.
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    private void updateKeyring(Session session, String email, CollectionOwner collectionOwner)
            throws IOException, NotFoundException, BadRequestException {
        User user = usersServiceSupplier.getService().getUserByEmail(email);
        if (session != null && user.keyring != null) {
            KeyManager.transferKeyring(user.keyring, zebedee.getKeyringCache().get(session), collectionOwner);
            usersServiceSupplier.getService().updateKeyring(user);
        }
    }
}
