package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by david on 24/04/2015.
 */
public class TeamsTest {

    Zebedee zebedee;
    Builder builder;
    Collection inflationCollection;
    Collection labourMarketCollection;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
        inflationCollection = new Collection(builder.collections.get(0), zebedee);
        labourMarketCollection = new Collection(builder.collections.get(1), zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }


    @Test
    public void shouldListTeams() throws IOException {

        // Given
        // The teams set up by the builder

        // When
        // We list the teams
        List<Team> teams = zebedee.teams.listTeams();

        // Then
        // We should have the two teams
        assertEquals(2, teams.size());
        for (Team team : teams) {
            assertTrue(builder.teamNames[0].equals(team.name) || builder.teamNames[1].equals(team.name));
        }
    }


    @Test
    public void shouldFindTeamByName() throws IOException, NotFoundException {

        // Given
        // The teams set up by the builder

        // When
        // We list the teams
        Team economyTeam = zebedee.teams.findTeam(builder.teamNames[0]);
        Team labourMarketTeam = zebedee.teams.findTeam(builder.teamNames[1]);

        // Then
        // We should find the teams
        assertNotNull(economyTeam);
        assertNotNull(labourMarketTeam);
        // and they should be the expected teams
        assertEquals(builder.teamNames[0], economyTeam.name);
        assertEquals(builder.teamNames[1], labourMarketTeam.name);
    }


    @Test
    public void shouldCreateTeam() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team name
        String name = "UK Trade and Intustry team";

        // When
        // We create the team
        Session session = zebedee.openSession(builder.administratorCredentials);
        Team ukTradeAndIndustryTeam = zebedee.teams.createTeam(name, session);

        // Then
        // We should be able to list the new team:
        List<Team> teams = zebedee.teams.listTeams();
        assertEquals(3, teams.size());
        boolean checked = false;
        for (Team team : teams) {
            if (team.id == ukTradeAndIndustryTeam.id) {
                assertEquals(name, team.name);
                checked = true;
            }
        }
        assertTrue(checked);
    }


    @Test
    public void shouldCreateTeamWithUniqueId() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A team name
        String name = "The Magic Roundabout Team";

        // When
        // We create a bunch of teams
        Session session = zebedee.openSession(builder.administratorCredentials);
        for (int i = 0; i < 10; i++) {
            zebedee.teams.createTeam(name + " " + i, session);
        }

        // Then
        // All teams should have a unique id
        Set<Integer> ids = new HashSet<>();
        for (Team team : zebedee.teams.listTeams()) {
            assertFalse(ids.contains(team.id));
            ids.add(team.id);
        }
    }


    @Test(expected = ConflictException.class)
    public void shouldNotCreateTeamWithDuplicateName() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team
        String name = "The twin project team";
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.teams.createTeam(name, session);

        // When
        // We create a team with a duplicate name
        zebedee.teams.createTeam(name, session);

        // Then
        // We should get a conflict exception
    }


    @Test(expected = UnauthorizedException.class)
    public void shouldNotCreateTeamIfNotAdministrator() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team name
        String name = "UK Trade and Intustry team";

        // When
        // We create the team
        Session session = zebedee.openSession(builder.reviewer1Credentials);
        Team ukTradeAndIndustryTeam = zebedee.teams.createTeam(name, session);

        // Then
        // We should get an unauthorized exception
    }


    @Test
    public void shouldRenameTeam() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team name
        String oldName = builder.teamNames[0];
        String newName = "Renamed team";
        Team team = zebedee.teams.findTeam(oldName);

        // When
        // We rename the team
        team.name = newName;
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.teams.renameTeam(team, session);

        // Then
        // We should still have the two teams, but with the new name
        List<Team> teams = zebedee.teams.listTeams();
        assertEquals(2, teams.size());
        for (Team candidate : teams) {
            assertTrue(newName.equals(team.name) || builder.teamNames[1].equals(team.name));
            assertFalse(oldName.equals(team.name));
        }
    }


    @Test(expected = ConflictException.class)
    public void shouldNotRenameTeamToDuplicateName() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team name
        String oldName = builder.teamNames[0];
        String duplicateName = builder.teamNames[1];
        Team team = zebedee.teams.findTeam(oldName);

        // When
        // We rename the team
        team.name = duplicateName;
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.teams.renameTeam(team, session);

        // Then
        // We should get a conflict exception
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotRenameNonexistentTeam() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // An updated team with an ID that doesn't exist
        Team team = new Team();
        team.name = "nonexistent";
        team.id = -1;
        team.members = new HashSet<>();

        // When
        // We rename the team
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.teams.renameTeam(team, session);

        // Then
        // We should get a not found exception
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotRenameToBlankName() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // An updated team with an ID that doesn't exist
        Team team = new Team();
        team.name = "";
        team.id = -1;
        team.members = new HashSet<>();

        // When
        // We rename the team
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.teams.renameTeam(team, session);

        // Then
        // We should get a bad request exception
    }


    @Test(expected = UnauthorizedException.class)
    public void shouldNotRenameTeamIfNotAdministrator() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team name
        String oldName = builder.teamNames[0];
        String newName = "Renamed team";
        Team team = zebedee.teams.findTeam(oldName);

        // When
        // We rename the team
        team.name = newName;
        Session session = zebedee.openSession(builder.reviewer2Credentials);
        zebedee.teams.renameTeam(team, session);

        // Then
        // We should have an unauthorized exception
    }


    @Test
    public void shouldDeleteTeam() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // One of the teams
        Team team = zebedee.teams.findTeam(builder.teamNames[0]);

        // When
        // We delete the team
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.teams.deleteTeam(team, session);

        // Then
        // We should only have the other team left
        List<Team> teams = zebedee.teams.listTeams();
        assertEquals(1, teams.size());
        assertTrue(builder.teamNames[1].equals(teams.get(0).name));
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonexistentTeam() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // An updated team with an ID that doesn't exist
        Team team = new Team();
        team.name = "nonexistent";
        team.id = -1;
        team.members = new HashSet<>();

        // When
        // We rename the team
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.teams.deleteTeam(team, session);

        // Then
        // We should get a not found exception
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotDeleteNullTeam() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // An updated team with an ID that doesn't exist
        Team team = null;

        // When
        // We rename the team
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.teams.renameTeam(team, session);

        // Then
        // We should get a bad request exception
    }


    @Test(expected = UnauthorizedException.class)
public void shouldNotDeleteTeamIfNotAdministrator() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A team to delete
        Team team = zebedee.teams.findTeam(builder.teamNames[0]);

        // When
        // We attempt to delete the team
        Session session = zebedee.openSession(builder.reviewer2Credentials);
        zebedee.teams.deleteTeam(team, session);

        // Then
        // We should have an unauthorized exception
    }


    @Test
    public void shouldAddTeamMember() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team
        String name = "My team";
        Session session = zebedee.openSession(builder.administratorCredentials);
        Team team = zebedee.teams.createTeam(name, session);

        // When
        // We add a team member
        zebedee.teams.addTeamMember(builder.reviewer2.email, team, session);

        // Then
        // The member should be present in the team
        Team read = zebedee.teams.findTeam(name);
        assertEquals(1, read.members.size());
        assertTrue(read.members.contains(builder.reviewer2.email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddTeamMemberIfNotAdministrator() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team
        String name = "My team";
        Session adminSession = zebedee.openSession(builder.administratorCredentials);
        Team team = zebedee.teams.createTeam(name, adminSession);

        // When
        // We add a team member without being an administrator
        Session session = zebedee.openSession(builder.reviewer1Credentials);
        zebedee.teams.addTeamMember(builder.reviewer1.email, team, session);

        // Then
        // We should have an unauthorized exception
    }


    @Test
    public void shouldRemoveTeamMember() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team
        String name = "My team";
        Session session = zebedee.openSession(builder.administratorCredentials);
        Team team = zebedee.teams.createTeam(name, session);
        zebedee.teams.addTeamMember(builder.reviewer1.email, team, session);
        zebedee.teams.addTeamMember(builder.reviewer2.email, team, session);

        // When
        // We remove a team member
        zebedee.teams.removeTeamMember(builder.reviewer1.email, team, session);

        // Then
        // The member should be present in the team
        Team read = zebedee.teams.findTeam(name);
        assertEquals(1, read.members.size());
        assertTrue(read.members.contains(builder.reviewer2.email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemoveTeamMemberIfNotAdministrator() throws IOException, UnauthorizedException, BadRequestException, ConflictException, NotFoundException {

        // Given
        // A new team
        String name = "My team";
        Session adminSession = zebedee.openSession(builder.administratorCredentials);
        Team team = zebedee.teams.createTeam(name, adminSession);
        zebedee.teams.addTeamMember(builder.reviewer1.email, team, adminSession);
        zebedee.teams.addTeamMember(builder.reviewer2.email, team, adminSession);

        // When
        // We add a team member without being an administrator
        Session session = zebedee.openSession(builder.reviewer1Credentials);
        zebedee.teams.removeTeamMember(builder.reviewer2.email, team, session);

        // Then
        // We should have an unauthorized exception
    }
}
