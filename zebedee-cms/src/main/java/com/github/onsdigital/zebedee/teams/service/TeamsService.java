package com.github.onsdigital.zebedee.teams.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Set;

/**
 * @deprecated this service is deprecated in favour of the dp-identity-api. Any additional code added should avoid using
 *             service directly.
 * TODO: Remove this service after the migration to the JWT sessions is complete
 */
@Deprecated
public interface TeamsService {

    /**
     * Return a list of the current teams.
     *
     * @throws IOException unexpected error listing the current teams.
     *
     * @deprecated as teams management is moving to the dp-identity-api this method can be removed when the teams
     *             endpoints are removed.
     */
    @Deprecated
    List<Team> listTeams() throws IOException;

    /**
     * @param teamIds
     * @return
     * @throws IOException
     *
     * @deprecated as teams management is moving to the dp-identity-api and the references to this method will be
     *             removed.
     */
    @Deprecated
    List<Team> resolveTeams(Set<Integer> teamIds) throws IOException;

    /**
     * Return a list of {@link Team} matching the IDS provided containing only the team name & ID.
     * @param teamIds the ID of the {@link Team}s to get
     *
     * @deprecated as Florence can request the team names from the dp-identity-api for presentation on the front end
     */
    @Deprecated
    List<Team> resolveTeamDetails(Set<Integer> teamIds) throws IOException;

    /**
     * Find a team by name.
     *
     * @param teamName the name of the team to search for.
     * @return
     * @throws IOException
     * @throws NotFoundException
     *
     * @deprecated as this is actually a legacy carry over. The collection endpoints are accepting the teams array as a
     *             list of team names, but according to the comment in {@link com.github.onsdigital.zebedee.json.CollectionBase}
     *             the array was always intended to be an array of IDs therefore negating the need for this method.
     *             This method will be removed after migration to the dp-identity-api.
     */
    @Deprecated
    Team findTeam(String teamName) throws IOException, NotFoundException;

    /**
     * Create a new content owner (viewer) team.
     *
     * @param teamName The name for the team.
     * @param session  Only administrators can create a team.
     * @return The created team.
     * @throws IOException If a filesystem error occurs.
     *
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    Team createTeam(String teamName, Session session) throws IOException, UnauthorizedException, ConflictException, NotFoundException, ForbiddenException;

    /**
     * Delete a team.
     *
     * @param delete  The team to be deleted. The ID will be used to find the existing team.
     * @param session Only an administrator can delete a team.
     * @throws IOException If a filesystem error occurs.
     *
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    void deleteTeam(Team delete, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException, ForbiddenException;

    /**
     * Adds the specified user to the content administrators, giving them access to read content at the given paths and all sub-paths.
     *
     * @param email The user's email.
     * @param team  The team to add the given email to.
     * @throws IOException If a filesystem error occurs.
     *
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    void addTeamMember(String email, Team team, Session session) throws IOException, UnauthorizedException, NotFoundException, ForbiddenException;

    /**
     * Adds the specified user to the content administrators, giving them access to read content at the given paths and all sub-paths.
     *
     * @param email The user's email.
     * @param team  The team to remove the given email from.
     * @throws IOException If a filesystem error occurs.
     *
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    void removeTeamMember(String email, Team team, Session session) throws IOException, UnauthorizedException, NotFoundException, ForbiddenException;

    /**
     * @return on the fly mapping of which teams currently contain which users. Format is teamName -> userEmail.
     * @throws IOException
     *
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    List<AbstractMap.SimpleEntry<String, String>> getTeamMembersSummary(Session session) throws IOException,
            UnauthorizedException, ForbiddenException;
}
