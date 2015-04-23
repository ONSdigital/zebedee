package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.AccessMapping;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Handles permissions mapping between users and {@link com.github.onsdigital.zebedee.Zebedee} functions.
 * Created by david on 12/03/2015.
 */
public class Permissions {
    private Path teamsPath;
    private Path accessMappingPath;
    private ReadWriteLock accessMappingLock = new ReentrantReadWriteLock();
    private ReadWriteLock teamLock = new ReentrantReadWriteLock();

    public Permissions(Path permissions, Path teams) {
        this.teamsPath = teams;
        accessMappingPath = permissions.resolve("accessMapping.json");
        System.out.println("Access mapping path: " + accessMappingPath);
    }

    /**
     * Determines whether the specified user has administator permissions.
     *
     * @param email The user's emal.
     * @return True if the user is an administrator.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean isAdministrator(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
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
    public void addAdministrator(String email, Session session) throws IOException {

        // Allow the initial user to be set as an administrator:
        if (hasAdministrator() && (session == null || !isAdministrator(session.email))) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
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
    public void removeAdministrator(String email, Session session) throws IOException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
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
     * @param email The user's email.
     * @return True if the user is a member of the Digital Publishing team.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean canEdit(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return canEdit(email, accessMapping);
    }

    /**
     * Adds the specified user to the Digital Publishing team, giving them access to read and write all content.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void addEditor(String email, Session session) throws IOException {
        if (hasAdministrator() && !isAdministrator(session.email)) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.digitalPublishingTeam == null) {
            accessMapping.digitalPublishingTeam = new HashSet<>();
        }
        accessMapping.digitalPublishingTeam.add(email);
        writeAccessMapping(accessMapping);
    }


    /**
     * Removes the specified user to the Digital Publishing team, revoking access to read and write all content.
     *
     * @param email The user's email.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeEditor(String email, Session session) throws IOException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
        }

        AccessMapping accessMapping = readAccessMapping();
        if (accessMapping.digitalPublishingTeam == null) {
            accessMapping.digitalPublishingTeam = new HashSet<>();
        }
        accessMapping.digitalPublishingTeam.remove(email);
        writeAccessMapping(accessMapping);
    }

    /**
     * Determines whether the specified user has viewing rights.
     *
     * @param email                 The user's email.
     * @param collectionDescription The collection to check access for.
     * @return True if the user is a member of the Digital Publishing team or
     * the user is a content owner with access to the given path or any parent path.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean canView(String email, CollectionDescription collectionDescription) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return collectionDescription != null && (canEdit(email, accessMapping) || canView(email, collectionDescription, accessMapping));
    }

    /**
     * Grants the given team access to the given collection.
     *
     * @param collectionDescription The collection to give the team access to.
     * @param team                  The team to be granted access.
     * @param session               Only editors can grant a team access to a collection.
     * @throws IOException If a filesystem error occurs.
     */
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException {
        if (session == null || !canEdit(session.email)) {
            return;
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
     * Revokes access for given team to the given collection.
     *
     * @param collectionDescription The collection to revoke team access to.
     * @param team                  The team to be revoked access.
     * @param session               Only editors can revoke team access to a collection.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException {
        if (session == null || !canEdit(session.email)) {
            return;
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

    public List<Team> listTeams() throws IOException {
        List<Team> result = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(teamsPath)) {
            for (Path path : stream) {
                teamLock.readLock().lock();
                try (InputStream input = Files.newInputStream(path)) {
                    result.add(Serialiser.deserialise(input, Team.class));
                } finally {
                    teamLock.readLock().unlock();
                }
            }
        }

        return result;
    }

    public Team findTeam(String teamName) throws IOException {
        Team result = null;

        for (Team team : listTeams()) {
            if (StringUtils.equals(team.name, teamName)) result = team;
        }

        return result;
    }

    /**
     * Create a new content owner (viewer) team.
     *
     * @param teamName The name for the team.
     * @param session  Only administrators can create a team.
     * @return The created team.
     * @throws IOException If a filesystem error occurs.
     */
    public Team createTeam(String teamName, Session session) throws IOException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
        }

        String filename = PathUtils.toFilename(teamName);
        int id = 0;

        // Check for a name conflict:
        List<Team> teams = listTeams();
        for (Team team : teams) {
            id = Math.max(id, team.id);
            String teamFilename = PathUtils.toFilename(team.name);
            if (StringUtils.equalsIgnoreCase(filename, teamFilename)) {
                throw new ConflictException("There is already a team matching this name.");
            }
        }

        // Create the team object:
        Team team = new Team();
        team.name = teamName;
        team.id = ++id;
        team.members = new HashSet<>();
        writeTeam(team);

        return team;
    }

    /**
     * Renames a team.
     *
     * @param update  The team with the updated name. The ID will be used to find the existing team.
     * @param session Only administrators can rename a team.
     * @throws IOException If a filesystem error occurs.
     */
    public void renameTeam(Team update, Session session) throws IOException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
        }

        if (update != null && StringUtils.isNotBlank(update.name)) {

            // Find the team to update:
            Team existing = null;
            List<Team> teams = listTeams();
            for (Team team : teams) {
                if (update.id == team.id) {
                    existing = update;
                }
            }
            if (existing == null) {
                throw new NotFoundException("Team ID not found: " + update);
            }

            // Create a file with the new name and then delete the old one:
            if (!StringUtils.equals(PathUtils.toFilename(existing.name), PathUtils.toFilename(update.name))) {
                writeTeam(update);
                Files.delete(teamPath(existing));
            }

        } else {
            throw new BadRequestException("Invalid team: " + update);
        }
    }

    /**
     * Delete a team.
     *
     * @param delete  The team to be deleted. The ID will be used to find the existing team.
     * @param session Only an administrator can delete a team.
     * @throws IOException If a filesystem error occurs.
     */
    public void deleteTeam(Team delete, Session session) throws IOException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
        }

        if (delete != null && StringUtils.isNotBlank(delete.name)) {

            // Find the team to update:
            Team existing = null;
            List<Team> teams = listTeams();
            for (Team team : teams) {
                if (delete.id == team.id) {
                    existing = delete;
                }
            }
            if (existing == null) {
                throw new NotFoundException("Team ID not found: " + delete);
            }

            // Delete the team:
            Files.delete(teamPath(existing));

        } else {
            throw new BadRequestException("Invalid team: " + delete);
        }
    }

    /**
     * Adds the specified user to the content administrators, giving them access to read content at the given paths and all sub-paths.
     *
     * @param email The user's email.
     * @param team  The team to add the given email to.
     * @throws IOException If a filesystem error occurs.
     */
    public void addTeamMember(String email, Team team, Session session) throws IOException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
        }

        if (!StringUtils.isBlank(email) && team != null) {
            team.members.add(standardise(email));
            writeTeam(team);
        }

    }

    /**
     * Adds the specified user to the content administrators, giving them access to read content at the given paths and all sub-paths.
     *
     * @param email The user's email.
     * @param team  The team to remove the given email from.
     * @throws IOException If a filesystem error occurs.
     */
    public void removeTeamMember(String email, Team team, Session session) throws IOException {
        if (session == null || !isAdministrator(session.email)) {
            throw new UnauthorizedException("Session is not an administrator: " + session);
        }

        if (!StringUtils.isBlank(email) && team != null) {
            team.members.remove(standardise(email));
            writeTeam(team);
        }

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
            for (Team team : listTeams()) {
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

    /**
     * Retrieves the given {@link Team}, creating it if necessary.
     *
     * @param teamName The name of the team to retrieve.
     * @return A {@link Team} with the given name.
     * @throws IOException If a filesystem error occurs.
     */
    private Team readTeam(String teamName) throws IOException {
        Team result = null;

        Path path = teamPath(teamName);
        if (path != null) {
            if (Files.exists(path)) {

                // Read the team
                teamLock.readLock().lock();
                try (InputStream input = Files.newInputStream(path)) {
                    result = Serialiser.deserialise(input, Team.class);
                } finally {
                    teamLock.readLock().unlock();
                }

                // Initialise the memers set if it's missing:
                if (result.members == null) {
                    result.members = new HashSet<>();
                }

            } else {

                // Generate a new team:
                Team team = new Team();
                team.name = teamName;
                team.members = new HashSet<>();
                writeTeam(team);
            }

        } else {
            throw new BadRequestException("Invalid team name: " + teamName);
        }

        return result;
    }

    private void writeTeam(Team team) throws IOException {

        Path path = teamPath(team);

        if (path != null) {
            teamLock.writeLock().lock();
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, team);
            } finally {
                teamLock.writeLock().unlock();
            }
        } else {
            throw new BadRequestException("Invalid team: " + team);
        }
    }

    private Path teamPath(Team team) {
        Path result = null;
        if (team != null) {
            result = teamPath(team.name);
        }
        return result;
    }

    private Path teamPath(String teamName) {
        Path result = null;
        if (StringUtils.isNotBlank(teamName)) {
            result = teamsPath.resolve(PathUtils.toFilename(teamName) + ".json");
        }
        return result;
    }


    private String standardise(String email) {
        return PathUtils.standardise(email);
    }

}
