package com.github.onsdigital.zebedee.teams.store;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.teams.model.Team;

import java.io.IOException;
import java.util.List;

/**
 * @deprecated in favour of the dp-identity-api and will be removed after the migration to JWT sessions is complete.
 */
@Deprecated
public interface TeamsStore {

    /**
     * Get a {@link Team}.
     *
     * @param teamName
     * @return
     * @throws IOException
     * @throws NotFoundException
     */
    Team get(String teamName) throws IOException, NotFoundException;


    /**
     *
     * @param team
     * @throws IOException
     * @throws NotFoundException
     */
    void save(Team team) throws IOException, NotFoundException;

    /**
     *
     * @return
     * @throws IOException
     */
    List<Team> listTeams() throws IOException;

    /**
     *
     * @param teamName
     * @return
     * @throws IOException
     */
    boolean exists(String teamName) throws IOException;

    /**
     *
     * @param team
     * @return
     * @throws IOException
     * @throws NotFoundException
     */
    boolean deleteTeam(Team team) throws IOException, NotFoundException;
}
