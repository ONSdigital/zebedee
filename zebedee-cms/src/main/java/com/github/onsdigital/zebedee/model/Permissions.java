package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;

/**
 * Handles permissions mapping between users and {@link com.github.onsdigital.zebedee.Zebedee} functions.
 * Created by david on 12/03/2015.
 */
public class Permissions {
    private Zebedee zebedee;
    private Path accessMappingPath;
    private ReadWriteLock accessMappingLock = new ReentrantReadWriteLock();

    public Permissions(Path permissions, Zebedee zebedee) {
        this.zebedee = zebedee;
        accessMappingPath = permissions.resolve("accessMapping.json");
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
        AccessMapping accessMapping = readAccessMapping();
        return isAdministrator(email, accessMapping);
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
     *
     * Future-proofing only at present
     *
     * @param session the session
     * @param collectionDescription the collection description
     * @return
     * @throws IOException
     */
    public boolean canEdit(Session session, CollectionDescription collectionDescription) throws IOException {
        if (collectionDescription.isEncrypted) {
            return canEdit(session.email) && zebedee.keyringCache.get(session).list().contains(collectionDescription.id);
        } else {
            return canEdit(session.email);
        }
    }

    /**
     * Get whether a user can edit a collection
     *
     * @param user the user
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
     * @param email the user email
     * @param collectionDescription
     * @return
     * @throws IOException
     */
    public boolean canEdit(String email, CollectionDescription collectionDescription) throws IOException {
        try {
            return canEdit(zebedee.users.get(email), collectionDescription);
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
        User user = zebedee.users.get(email);
        if (session != null && user.keyring != null) {
            KeyManager.transferKeyring(user.keyring, zebedee.keyringCache.get(session));
            zebedee.users.updateKeyring(user);
        }
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
     **
     * @param user               The user. Can be null.
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
     * @param email               The email of the user
     * @param collectionDescription The collection to check access for.
     * @return True if the user is a member of the Digital Publishing team or
     * the user is a content owner with access to the given path or any parent path.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean canView(String email, CollectionDescription collectionDescription) throws IOException {

        try {
            return canView(zebedee.users.get(email), collectionDescription);
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
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, UnauthorizedException {
        if (session == null || !canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        Set<Integer> collectionTeams = accessMapping.collections.get(collectionDescription.id);
        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
            accessMapping.collections.put(collectionDescription.id, collectionTeams);
        }
        collectionTeams.add(team.id);
        writeAccessMapping(accessMapping);
    }

    /**
     * Provide a list of team ID's currently associated with a collection
     * @param collectionDescription
     * @param session
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     */
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws IOException, UnauthorizedException {
        if (session == null || !canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        return java.util.Collections.unmodifiableSet(accessMapping.collections.get(collectionDescription.id));
    }

    /**
     * Revokes access for given team to the given collection.
     *
     * @param collectionDescription The collection to revoke team access to.
     * @param team                  The team to be revoked access.
     * @param session               Only editors can revoke team access to a collection.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, UnauthorizedException {
        if (session == null || !canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        AccessMapping accessMapping = readAccessMapping();
        Set<Integer> collectionTeams = accessMapping.collections.get(collectionDescription.id);
        if (collectionTeams == null) {
            collectionTeams = new HashSet<>();
            accessMapping.collections.put(collectionDescription.id, collectionTeams);
        }
        collectionTeams.remove(team.id);
        writeAccessMapping(accessMapping);
    }

    private boolean canEdit(String email, AccessMapping accessMapping) throws IOException {
        Set<String> digitalPublishingTeam = accessMapping.digitalPublishingTeam;
        return digitalPublishingTeam != null && digitalPublishingTeam.contains(standardise(email));
    }

    private boolean canView(String email, CollectionDescription collectionDescription, AccessMapping accessMapping) throws IOException {
        boolean result = false;

        // Check to see if the email is a member of a team associated with the given collection:
        Set<Integer> teams = accessMapping.collections.get(collectionDescription.id);
        if (teams != null) {
            for (Team team : zebedee.teams.listTeams()) {
                if (teams.contains(team.id)) {
                    return team.members.contains(standardise(email));
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

        } else {

            // Or generate a new one:
            result = new AccessMapping();
            result.administrators = new HashSet<>();
            result.digitalPublishingTeam = new HashSet<>();
            result.collections = new HashMap<>();
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
     * @throws UnauthorizedException If the request is not from an admin
     */
    public PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException, UnauthorizedException {

        if ((session == null) || !isAdministrator(session.email) || !session.email.equals(email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        PermissionDefinition definition = new PermissionDefinition();
        definition.email = email;
        definition.admin = isAdministrator(email);
        definition.editor = canEdit(email);
        return definition;
    }
}
