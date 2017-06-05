package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsServiceImpl;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.json.Team;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.Zebedee.TEAMS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests verifing the behaviour of the {@link Teams} in both success and failure scenarios.
 */
public class TeamsTest {

    private static final String TEST_EMAIL = "test@ons.gov.uk";

    @Mock
    private Session sessionMock;

    @Mock
    private PermissionsServiceImpl permissionsServiceImplMock;

    @Rule
    public TemporaryFolder zebedeeRoot;

    private Teams teamsService;
    private Path teamsPath;
    private Team teamA;
    private Team teamB;
    private ServiceSupplier<PermissionsService> permissionsServiceSupplier;

    /**
     * Set up the test.
     */
    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        zebedeeRoot = new TemporaryFolder();
        zebedeeRoot.create();
        zebedeeRoot.newFolder("teams");

        permissionsServiceSupplier = () -> permissionsServiceImplMock;

        teamsPath = zebedeeRoot.getRoot().toPath().resolve(TEAMS);
        teamsService = new Teams(teamsPath, permissionsServiceSupplier);

        teamA = new Team()
                .setId(123)
                .setName("Team-A")
                .setMembers(new HashSet<>())
                .addMember("Dave")
                .addMember("Adrian")
                .addMember("Janick");

        teamB = new Team()
                .setId(456)
                .setName("Team-B")
                .setMembers(new HashSet<>())
                .addMember("Bruce")
                .addMember("Steve")
                .addMember("Nicko");
    }

    /**
     * Clean up.
     */
    @After
    public void cleanUp() {
        zebedeeRoot.delete();
    }

    @Test
    public void shouldListTeams() throws Exception {
        List<Team> teamsList = createTeams();
        assertThat(teamsService.listTeams(), equalTo(teamsList));
    }

    @Test
    public void shouldFindTeamByName() throws IOException, NotFoundException {
        List<Team> teamsList = createTeams();
        assertThat(teamsService.findTeam(teamA.getName()), equalTo(teamA));
    }

    @Test
    public void shouldCreateTeam() throws IOException, UnauthorizedException, BadRequestException, ConflictException,
            NotFoundException {
        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(true);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        Team result = teamsService.createTeam("TeamONS", sessionMock);
        assertThat(teamsPath.resolve("teamons.json").toFile().exists(), equalTo(true));

        Team target = teamsService.findTeam("TeamONS");
        assertThat(result, equalTo(target));

        verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
        verify(sessionMock, times(1)).getEmail();
    }

    @Test
    public void shouldCreateTeamWithUniqueId() throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {
        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(true);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        Set<Integer> ids = new HashSet<>();
        String name = "team%d";

        // Create 20 teams and add their ID to the set.
        for (int i = 0; i < 20; i++) {
            ids.add(teamsService.createTeam(String.format(name, i), sessionMock).getId());
        }

        // If there are 20 unique IDS the set will have the same number of entries as teams created.
        assertThat(ids.size(), equalTo(20));

        verify(permissionsServiceImplMock, times(20)).isAdministrator(TEST_EMAIL);
        verify(sessionMock, times(20)).getEmail();
    }

    @Test(expected = ConflictException.class)
    public void shouldNotCreateTeamWithDuplicateName() throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {
        createTeams();
        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(true);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        try {
            teamsService.createTeam("Team-A", sessionMock);
        } catch (ConflictException e) {
            assertThat(teamsService.listTeams().size(), equalTo(2));
            verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotCreateTeamIfNotAdministrator() throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {
        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(false);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        try {
            teamsService.createTeam("Non Admin team", sessionMock);
        } catch (UnauthorizedException e) {
            assertThat(teamsService.listTeams().isEmpty(), is(true));
            verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            throw e;
        }
    }

    @Test
    public void shouldDeleteTeam() throws IOException, UnauthorizedException, BadRequestException, ConflictException,
            NotFoundException {
        createTeams();
        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(true);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        teamsService.deleteTeam(teamA, sessionMock);

        List<Team> afterDelete = teamsService.listTeams();
        assertThat(afterDelete.size(), equalTo(1));
        assertThat(afterDelete
                        .stream()
                        .filter(t -> t.getName().equals(teamA.getName()) || t.getId() == teamA.getId())
                        .findAny()
                        .isPresent(),
                equalTo(false));

        verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
        verify(sessionMock, times(1)).getEmail();
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowExceptionIfTeamToDeleteDoesNotExist() throws IOException, UnauthorizedException,
            BadRequestException, ConflictException, NotFoundException {
        List<Team> initialTeams = createTeams();
        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(true);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        try {
            teamsService.deleteTeam(new Team().setId(999).setName("AGirlIsNoOne"), sessionMock);
        } catch (NotFoundException e) {
            assertThat(initialTeams, equalTo(teamsService.listTeams()));
            verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotDeleteNullTeam() throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {
        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(true);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        try {
            teamsService.deleteTeam(null, sessionMock);
        } catch (BadRequestException e) {
            verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotDeleteTeamIfNotAdministrator() throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {
        List<Team> initialTeams = createTeams();
        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(false);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        try {
            teamsService.deleteTeam(teamA, sessionMock);
        } catch (UnauthorizedException e) {
            List<Team> afterDelete = teamsService.listTeams();
            assertThat(afterDelete, equalTo(initialTeams));
            assertThat(afterDelete.contains(teamA), is(true));
            verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            throw e;
        }
    }

    @Test
    public void shouldAddTeamMember() throws IOException, UnauthorizedException, BadRequestException, ConflictException,
            NotFoundException {
        List<Team> initial = createTeams();
        int teamAMembers = teamA.getMembers().size();

        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(true);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        teamsService.addTeamMember(TEST_EMAIL, teamA, sessionMock);
        Team teamAUpdated = teamsService.findTeam(teamA.getName());
        assertThat(teamAUpdated.getMembers().contains(TEST_EMAIL), equalTo(true));
        assertThat(teamAUpdated.getMembers().size(), equalTo(++teamAMembers));

        verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
        verify(sessionMock, times(1)).getEmail();
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddTeamMemberIfNotAdministrator() throws IOException, UnauthorizedException,
            BadRequestException, ConflictException, NotFoundException {
        createTeams();
        Team initial = teamA;

        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(false);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        try {
            teamsService.addTeamMember(TEST_EMAIL, teamA, sessionMock);
        } catch (UnauthorizedException e) {
            Team updated = teamsService.findTeam(teamA.getName());
            assertThat(updated.getMembers(), equalTo(initial.getMembers()));
            verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            throw e;
        }
    }

    @Test
    public void shouldRemoveTeamMember() throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {
        createTeams();
        Team inital = teamA;

        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(true);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        teamsService.removeTeamMember("Dave", teamA, sessionMock);

        Team updated = teamsService.findTeam(teamA.getName());
        assertThat(!updated.getMembers().contains("Dave"), is(false));

        verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
        verify(sessionMock, times(1)).getEmail();
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemoveTeamMemberIfNotAdministrator() throws IOException, UnauthorizedException,
            BadRequestException, ConflictException, NotFoundException {
        createTeams();
        Team inital = teamA;

        when(permissionsServiceImplMock.isAdministrator(TEST_EMAIL))
                .thenReturn(false);
        when(sessionMock.getEmail())
                .thenReturn(TEST_EMAIL);

        try {
            teamsService.removeTeamMember("Dave", teamA, sessionMock);
        } catch (UnauthorizedException e) {
            Team updated = teamsService.findTeam(teamA.getName());
            assertThat(updated.getMembers().contains("Dave"), is(true));

            verify(permissionsServiceImplMock, times(1)).isAdministrator(TEST_EMAIL);
            verify(sessionMock, times(1)).getEmail();
            throw e;
        }
    }

    private void writeTeams(List<Team> teanList) throws IOException {
        for (Team t : teanList) {
            File f = teamsPath.resolve(t.getName().replace("-", "").toLowerCase().trim() + ".json").toFile();
            f.createNewFile();

            try (FileOutputStream fos = new FileOutputStream(f, false)) {
                fos.write(new Gson().toJson(t).getBytes());
                fos.flush();
            }
        }
    }

    private List<Team> createTeams() throws IOException {
        List<Team> teamsList = new ArrayList<>();
        teamsList.add(teamA);
        teamsList.add(teamB);
        writeTeams(teamsList);
        return teamsList;
    }
}
