package com.github.onsdigital.zebedee.teams.model;


import com.github.onsdigital.zebedee.teams.model.Team;

import java.util.List;

/**
 * Created by david on 21/04/2015.
 */
public class TeamList {

    private List<Team> teams;

    public TeamList(List<Team> teamList) {
        this.teams = teamList;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }
}
