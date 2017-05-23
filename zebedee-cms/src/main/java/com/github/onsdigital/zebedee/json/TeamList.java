package com.github.onsdigital.zebedee.json;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 21/04/2015.
 */
public class TeamList {
    public class TeamListTeam {
        public int id;
        public String name;

        public TeamListTeam(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public List<TeamListTeam> teams;

    public TeamList(List<Team> teamList) {
        teams = new ArrayList<>();
        for (Team team : teamList) {
            teams.add(new TeamListTeam(team.id, team.name));
        }
    }
}

