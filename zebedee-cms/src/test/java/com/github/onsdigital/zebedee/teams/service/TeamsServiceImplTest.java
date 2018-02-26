package com.github.onsdigital.zebedee.teams.service;

import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.store.TeamsStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test cases verifying the success and failure scenarios of the {@link TeamsServiceImpl}.
 */
public class TeamsServiceImplTest {

    private static final String EMAIL = "admin@ons.gov.uk";
    private static final String TEAM_D_NAME = "teamD";

    @Mock
    private TeamsStore teamsStore;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private ReadWriteLock readWriteLock;

    @Mock
    private Lock lock;

    private Session session;
    private TeamsService service;
    private ServiceSupplier<PermissionsService> permissionsServiceServiceSupplier;
    private Team teamA;
    private Team teamB;
    private Team teamC;
    private List<Team> teamsList;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        teamA = new Team().setId(10001).setName("teamA");
        teamB = new Team().setId(10002).setName("teamB");
        teamC = new Team().setId(10003).setName("teamC");
        teamsList = new ArrayList<>();
        session = new Session();

        permissionsServiceServiceSupplier = () -> permissionsService;

        service = new TeamsServiceImpl(teamsStore, () -> permissionsService);

        when(readWriteLock.readLock())
                .thenReturn(lock);
        when(readWriteLock.writeLock())
                .thenReturn(lock);

        ReflectionTestUtils.setField(service, "teamLock", readWriteLock);
    }

    @Test
    public void resolveTeams_ShouldFindRequestedTeams() throws Exception {
        Set<Integer> requestedTeamIDs = new HashSet<>();
        requestedTeamIDs.add(teamA.getId());
        requestedTeamIDs.add(teamB.getId());

        teamsList.add(teamA);
        teamsList.add(teamB);
        teamsList.add(teamC);

        when(teamsStore.listTeams())
                .thenReturn(teamsList);

        List<Team> result = service.resolveTeams(requestedTeamIDs);
        List<Team> expected = new ArrayList<>();
        expected.add(teamA);
        expected.add(teamB);

        assertThat(result, equalTo(expected));

        verify(teamsStore, times(1)).listTeams();
    }

    @Test
    public void resolveTeamDetails_success() throws Exception {
        Set<Integer> requestedTeamIDs = new HashSet<>();
        requestedTeamIDs.add(teamA.getId());
        requestedTeamIDs.add(teamB.getId());

        teamsList.add(teamA);
        teamsList.add(teamB);

        List<Team> expected = teamsList.stream()
                .map(team -> new Team().setName(team.getName()).setId(team.getId()))
                .collect(Collectors.toList());

        when(teamsStore.listTeams())
                .thenReturn(teamsList);

        List<Team> result = service.resolveTeamDetails(requestedTeamIDs);

        assertThat(result, equalTo(expected));

        verify(teamsStore, times(1)).listTeams();
    }

    @Test
    public void findTeam_Success() throws Exception {
        when(teamsStore.get(teamA.getName()))
                .thenReturn(teamA);

        assertThat(teamA, equalTo(service.findTeam(teamA.getName())));
        verify(teamsStore, times(1)).get(teamA.getName());
    }

    @Test(expected = IOException.class)
    public void findTeam_ShouldPropagteTeamsStoreException() throws Exception {
        when(teamsStore.get(teamA.getName()))
                .thenThrow(new IOException());
        try {
            service.findTeam(teamA.getName());
        } catch (IOException e) {
            verify(teamsStore, times(1)).get(teamA.getName());
            throw e;
        }
    }

    @Test
    public void listTeams_Success() throws Exception {
        when(teamsStore.listTeams())
                .thenReturn(teamsList);

        assertThat(teamsList, equalTo(service.listTeams()));
        verify(teamsStore, times(1)).listTeams();
    }

    @Test(expected = IOException.class)
    public void listTeams_ShouldProgagateTeamsStoreException() throws Exception {
        when(teamsStore.listTeams())
                .thenThrow(new IOException());

        try {
            service.listTeams();
        } catch (IOException e) {
            verify(teamsStore, times(1)).listTeams();
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void createTeam_ShouldThrowExeIfSessionNull() throws Exception {
        try {
            service.createTeam(TEAM_D_NAME, null);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(permissionsService, teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void createTeam_ShouldThrowExeIfSessionEmailNull() throws Exception {
        try {
            service.createTeam(TEAM_D_NAME, session);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(permissionsService, teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test(expected = ForbiddenException.class)
    public void createTeam_ShouldThrowExeIfUserNotAdmin() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(false);

        try {
            service.createTeam(TEAM_D_NAME, session);
        } catch (ForbiddenException e) {
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verifyNoMoreInteractions(permissionsService);
            verifyZeroInteractions(teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test(expected = ConflictException.class)
    public void createTeam_ShouldThrowExeIfTeamExistsAlready() throws Exception {
        session.setEmail(EMAIL);

        when(teamsStore.exists(TEAM_D_NAME))
                .thenReturn(true);
        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);

        try {
            service.createTeam(TEAM_D_NAME, session);
        } catch (ConflictException e) {
            verify(teamsStore, times(1)).exists(TEAM_D_NAME);
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verifyNoMoreInteractions(teamsStore, permissionsService);
            verifyZeroInteractions(readWriteLock, lock);
            throw e;
        }
    }

    @Test
    public void createTeam_Success() throws Exception {
        session.setEmail(EMAIL);
        teamsList.add(teamA);
        teamsList.add(teamB);
        teamsList.add(teamC);

        Team expected = new Team()
                .setId(10004)
                .setName(TEAM_D_NAME);

        when(teamsStore.exists(TEAM_D_NAME))
                .thenReturn(false);
        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);
        when(teamsStore.listTeams())
                .thenReturn(teamsList);

        Team result = service.createTeam(TEAM_D_NAME, session);

        assertThat(result, equalTo(expected));
        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verify(readWriteLock, times(2)).writeLock();
        verify(teamsStore, times(1)).listTeams();
        verify(teamsStore, times(1)).save(expected);
        verify(lock, times(1)).lock();
        verify(lock, times(1)).unlock();
    }

    @Test(expected = IOException.class)
    public void createTeam_ShouldReleaseLockAndProgagateException() throws Exception {
        session.setEmail(EMAIL);
        teamsList.add(teamA);
        teamsList.add(teamB);
        teamsList.add(teamC);
        Team expected = new Team()
                .setId(10004)
                .setName(TEAM_D_NAME);

        when(teamsStore.exists(TEAM_D_NAME))
                .thenReturn(false);
        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);
        when(teamsStore.listTeams())
                .thenReturn(teamsList);
        doThrow(new IOException())
                .when(teamsStore).save(expected);

        try {
            Team result = service.createTeam(TEAM_D_NAME, session);
        } catch (IOException e) {
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verify(readWriteLock, times(2)).writeLock();
            verify(teamsStore, times(1)).listTeams();
            verify(teamsStore, times(1)).save(expected);
            verify(lock, times(1)).lock();
            verify(lock, times(1)).unlock();
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void deleteTeam_ShouldThrowExeIfSessionNull() throws Exception {
        try {
            service.deleteTeam(teamA, null);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(permissionsService, teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void deleteTeam_ShouldThrowExeIfSessionEmailNull() throws Exception {
        try {
            service.deleteTeam(teamA, session);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(permissionsService, teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test(expected = ForbiddenException.class)
    public void deleteTeam_ShouldThrowExeUserNotAdmin() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(false);

        try {
            service.deleteTeam(teamA, session);
        } catch (ForbiddenException e) {
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verifyNoMoreInteractions(permissionsService);
            verifyZeroInteractions(teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void deleteTest_ShouldPropagteTeamsStoreException() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);
        when(teamsStore.deleteTeam(teamA))
                .thenThrow(new IOException());

        try {
            service.deleteTeam(teamA, session);
        } catch (IOException e) {
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verify(teamsStore, times(1)).deleteTeam(teamA);
            verifyZeroInteractions(readWriteLock, lock);
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void deleteTest_ShouldThrowExceptionIfDeleteUncessucessful() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);
        when(teamsStore.deleteTeam(teamA))
                .thenReturn(false);

        try {
            service.deleteTeam(teamA, session);
        } catch (IOException e) {
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verify(teamsStore, times(1)).deleteTeam(teamA);
            verifyZeroInteractions(readWriteLock, lock);
            throw e;
        }
    }

    @Test
    public void deleteTest_Success() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);
        when(teamsStore.deleteTeam(teamA))
                .thenReturn(true);

        service.deleteTeam(teamA, session);
        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verify(teamsStore, times(1)).deleteTeam(teamA);
        verifyZeroInteractions(readWriteLock, lock);
    }

    @Test (expected = UnauthorizedException.class)
    public void addTeamMemeber_ShouldThrowExIfSessionNull() throws Exception {
        try {
            service.addTeamMember(EMAIL, teamA, null);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(permissionsService, teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test (expected = UnauthorizedException.class)
    public void addTeamMemeber_ShouldThrowExIfSessionEmailNull() throws Exception {
        try {
            service.addTeamMember(EMAIL, teamA, session);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(permissionsService, teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test (expected = ForbiddenException.class)
    public void addTeamMemeber_ShouldThrowExIfDUserNotAdmin() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(false);

        try {
            service.addTeamMember(EMAIL, teamA, session);
        } catch (ForbiddenException e) {
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verifyZeroInteractions(teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test
    public void addTeamMember_ShouldNotAddIfEmailNull() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);

        service.addTeamMember(null, teamA, session);

        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verifyZeroInteractions(teamsStore, readWriteLock, lock);
    }

    @Test
    public void addTeamMember_ShouldNotAddIfTeamNull() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);

        service.addTeamMember(EMAIL, null, session);

        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verifyZeroInteractions(teamsStore, readWriteLock, lock);
    }

    @Test
    public void addTeamMember_Success() throws Exception {
        session.setEmail(EMAIL);
        Team target = mock(Team.class);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);
        when(target.addMember(EMAIL))
                .thenReturn(target);

        service.addTeamMember(EMAIL, target, session);

        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verify(target, times(1)).addMember(EMAIL);
        verify(teamsStore, times(1)).save(target);
        verify(readWriteLock, times(2)).writeLock();
        verify(lock, times(1)).lock();
        verify(lock, times(1)).unlock();
    }

    @Test (expected = UnauthorizedException.class)
    public void removeTeamMember_ShouldThrowExIfSessionNull() throws Exception {
        try {
            service.removeTeamMember(EMAIL, teamA, null);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(permissionsService, teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test (expected = UnauthorizedException.class)
    public void removeTeamMember_ShouldThrowExIfSessionEmailNull() throws Exception {
        try {
            service.removeTeamMember(EMAIL, teamA, session);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(permissionsService, teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test (expected = ForbiddenException.class)
    public void removeTeamMember_ShouldThrowExIfUserNotAdmin() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(false);

        try {
            service.removeTeamMember(EMAIL, teamA, session);
        } catch (ForbiddenException e) {
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verifyZeroInteractions(teamsStore, readWriteLock, lock);
            throw e;
        }
    }

    @Test
    public void removeTeamMember_ShouldNotRemoveIfEmailNull() throws Exception {
        session.setEmail(EMAIL);

        Team target = mock(Team.class);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);

        service.removeTeamMember(null, target, session);

        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verifyZeroInteractions(target, teamsStore, readWriteLock, lock);
    }

    @Test
    public void removeTeamMember_ShouldNotRemoveIfTeamNull() throws Exception {
        session.setEmail(EMAIL);

        Team target = mock(Team.class);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);

        service.removeTeamMember(EMAIL, null, session);

        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verifyZeroInteractions(target, teamsStore, readWriteLock, lock);
    }

    @Test
    public void removeTeamMember_Success() throws Exception {
        session.setEmail(EMAIL);
        Team target = mock(Team.class);

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);
        when(target.removeMember(EMAIL))
                .thenReturn(target);

        service.removeTeamMember(EMAIL, target, session);

        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verify(target, times(1)).removeMember(EMAIL);
        verify(teamsStore, times(1)).save(target);
        verify(readWriteLock, times(2)).writeLock();
        verify(lock, times(1)).lock();
        verify(lock, times(1)).unlock();
    }

    @Test (expected = UnauthorizedException.class)
    public void getTeamMembersSummary_ShouldThrowUnauthorizedExceptionIfSessionNull() throws Exception {
        try {
            service.getTeamMembersSummary(null);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(teamsStore, permissionsService);
            throw e;
        }
    }

    @Test (expected = UnauthorizedException.class)
    public void getTeamMembersSummary_ShouldThrowUnauthorizedExceptionIfSessionEmailNull() throws Exception {
        session.setEmail(null);
        try {
            service.getTeamMembersSummary(session);
        } catch (UnauthorizedException e) {
            verifyZeroInteractions(teamsStore, permissionsService);
            throw e;
        }
    }

    @Test (expected = ForbiddenException.class)
    public void getTeamMembersSummary_ShouldThrowUnauthorizedExceptionIfUserNotAdmin() throws Exception {
        session.setEmail(EMAIL);

        when(permissionsService.isAdministrator(session))
                .thenReturn(false);

        try {
            service.getTeamMembersSummary(session);
        } catch (ForbiddenException e) {
            verify(permissionsService, times(1)).isAdministrator(EMAIL);
            verifyZeroInteractions(teamsStore);
            throw e;
        }
    }

    @Test
    public void getTeamMembersSummary_Success() throws Exception {
        session.setEmail(EMAIL);

        teamsList.add(teamA.addMember("userA").addMember("userB"));
        teamsList.add(teamC.addMember("userC"));

        when(permissionsService.isAdministrator(EMAIL))
                .thenReturn(true);
        when(teamsStore.listTeams())
                .thenReturn(teamsList);

        List<AbstractMap.SimpleEntry<String, String>> expected = new ArrayList<>();
        expected.add(new AbstractMap.SimpleEntry<String, String>(teamA.getName(), "userA"));
        expected.add(new AbstractMap.SimpleEntry<String, String>(teamA.getName(), "userB"));
        expected.add(new AbstractMap.SimpleEntry<String, String>(teamC.getName(), "userC"));

        List<AbstractMap.SimpleEntry<String, String>> result = service.getTeamMembersSummary(session);

        assertThat(result, equalTo(expected));
        verify(permissionsService, times(1)).isAdministrator(EMAIL);
        verify(teamsStore, times(1)).listTeams();
        verifyNoMoreInteractions(permissionsService, teamsStore);
    }
}
