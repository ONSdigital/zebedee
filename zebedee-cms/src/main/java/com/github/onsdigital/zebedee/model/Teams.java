package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;

/**
 * Handles permissions mapping between users and {@link com.github.onsdigital.zebedee.Zebedee} functions.
 * Created by david on 12/03/2015.
 */
public class Teams {

    private Path teamsPath;
    private Permissions permissions;
    private ReadWriteLock teamLock = new ReentrantReadWriteLock();

    public Teams(Path teams, Permissions permissions) {
        this.teamsPath = teams;
        this.permissions = permissions;
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

    public List<Team> resolveTeams(Set<Integer> teamIds) throws IOException {
        List<Team> teams = listTeams();
        List<Team> resolvedTeams = new ArrayList<>();
        for (Integer currentTeamId : teamIds) { // for each current team ID
            for (Team team : teams) { // iterate the teams list to find the team object
                if (currentTeamId.equals(team.getId())) { // if the ID's match
                    resolvedTeams.add(team);
                }
            }
        }
        return resolvedTeams;
    }

    public Team findTeam(String teamName) throws IOException, NotFoundException {
        Team result = readTeam(teamName);
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
    public Team createTeam(String teamName, Session session) throws IOException, UnauthorizedException, ConflictException, NotFoundException {
        if (session == null || !permissions.isAdministrator(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        Path path = teamPath(teamName);
        int id = 0;

        // Check for a name conflict:
        if (Files.exists(path)) {
            throw new ConflictException("There is already a team matching this name.");
        }

        // Work out the next id:
        for (Team team : listTeams()) {
            id = Math.max(id, team.getId());
        }

        // Create the team object:
        Team team = new Team()
                .setName(teamName)
                .setId(++id);
        writeTeam(team);
        return team;
    }

    /**
     * Delete a team.
     *
     * @param delete  The team to be deleted. The ID will be used to find the existing team.
     * @param session Only an administrator can delete a team.
     * @throws IOException If a filesystem error occurs.
     */
    public void deleteTeam(Team delete, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        if (session == null || !permissions.isAdministrator(session.getEmail()))
            throw new UnauthorizedException(getUnauthorizedMessage(session));

        if (delete != null) {

            // Find the team to update:
            Team existing = null;
            List<Team> teams = listTeams();
            for (Team team : teams) {
                if (delete.getId() == team.getId()) {
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
    public void addTeamMember(String email, Team team, Session session) throws IOException, UnauthorizedException, NotFoundException {
        if (session == null || !permissions.isAdministrator(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        if (!StringUtils.isBlank(email) && team != null) {
            team.addMember(PathUtils.standardise(email));
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
    public void removeTeamMember(String email, Team team, Session session) throws IOException, UnauthorizedException, NotFoundException {
        if (session == null || !permissions.isAdministrator(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        if (!StringUtils.isBlank(email) && team != null) {
            team.getMembers().remove(PathUtils.standardise(email));
            writeTeam(team);
        }
    }


    /**
     * Retrieves the given {@link Team}, creating it if necessary.
     *
     * @param teamName The name of the team to retrieve.
     * @return A {@link Team} with the given name.
     * @throws IOException If a filesystem error occurs.
     */
    private boolean teamExists(String teamName) throws IOException {
        Path path = teamPath(teamName);
        return path != null && Files.exists(path);
    }


    /**
     * Retrieves the given {@link Team}, creating it if necessary.
     *
     * @param teamName The name of the team to retrieve.
     * @return A {@link Team} with the given name.
     * @throws IOException If a filesystem error occurs.
     */
    private Team readTeam(String teamName) throws IOException, NotFoundException {
        Team result = null;

        Path path = teamPath(teamName);
        if (path != null && Files.exists(path)) {

            // Read the team
            teamLock.readLock().lock();
            try (InputStream input = Files.newInputStream(path)) {
                result = Serialiser.deserialise(input, Team.class);
            } finally {
                teamLock.readLock().unlock();
            }

            // Initialise the memers set if it's missing:
            if (result.getMembers() == null) {
                result.setMembers(new HashSet<>());
            }

        } else {
            throw new NotFoundException("Team not found: " + teamName);
        }

        return result;
    }

    private void writeTeam(Team team) throws IOException, NotFoundException {

        Path path = teamPath(team);

        if (path != null) {
            teamLock.writeLock().lock();
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, team);
            } finally {
                teamLock.writeLock().unlock();
            }
        } else {
            throw new NotFoundException("Team not found: " + team);
        }
    }

    private Path teamPath(Team team) {
        Path result = null;
        if (team != null) {
            result = teamPath(team.getName());
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


}
