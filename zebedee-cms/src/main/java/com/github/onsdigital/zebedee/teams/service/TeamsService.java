package com.github.onsdigital.zebedee.teams.service;

import com.github.onsdigital.zebedee.exceptions.*;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Set;

/**
 * Created by dave on 08/06/2017.
 */
public interface TeamsService {

    /**
     * Return a list of the current teams.
     *
     * @throws IOException unexpected error listing the current teams.
     */
    List<Team> listTeams() throws IOException;

    /**
     * @param teamIds
     * @return
     * @throws IOException
     */
    List<Team> resolveTeams(Set<Integer> teamIds) throws IOException;

    /**
     * Return a list of {@link Team} matching the IDS provided containing only the team name & ID.
     * @param teamIds the ID of the {@link Team}s to get
     */
    List<Team> resolveTeamDetails(Set<Integer> teamIds) throws IOException;

    /**
     * Find a team by name.
     *
     * @param teamName the name of the team to search for.
     * @return
     * @throws IOException
     * @throws NotFoundException
     */
    Team findTeam(String teamName) throws IOException, NotFoundException;

    /**
     * Create a new content owner (viewer) team.
     *
     * @param teamName The name for the team.
     * @param session  Only administrators can create a team.
     * @return The created team.
     * @throws IOException If a filesystem error occurs.
     */
    Team createTeam(String teamName, Session session) throws IOException, UnauthorizedException, ConflictException, NotFoundException, ForbiddenException;

    /**
     * Delete a team.
     *
     * @param delete  The team to be deleted. The ID will be used to find the existing team.
     * @param session Only an administrator can delete a team.
     * @throws IOException If a filesystem error occurs.
     */
    void deleteTeam(Team delete, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException, ForbiddenException;

    /**
     * Adds the specified user to the content administrators, giving them access to read content at the given paths and all sub-paths.
     *
     * @param email The user's email.
     * @param team  The team to add the given email to.
     * @throws IOException If a filesystem error occurs.
     */
    void addTeamMember(String email, Team team, Session session) throws IOException, UnauthorizedException, NotFoundException, ForbiddenException;

    /**
     * Adds the specified user to the content administrators, giving them access to read content at the given paths and all sub-paths.
     *
     * @param email The user's email.
     * @param team  The team to remove the given email from.
     * @throws IOException If a filesystem error occurs.
     */
    void removeTeamMember(String email, Team team, Session session) throws IOException, UnauthorizedException, NotFoundException, ForbiddenException;

    /**
     * @return on the fly mapping of which teams currently contain which users. Format is teamName -> userEmail.
     * @throws IOException
     */
    List<AbstractMap.SimpleEntry<String, String>> getTeamMembersSummary(Session session) throws IOException,
            UnauthorizedException, ForbiddenException;
}
