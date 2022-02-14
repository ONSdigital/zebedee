package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by dave on 31/05/2017.
 */
public class PermissionsServiceImplTest {

    private static final String EMAIL = "admin@ons.gov.uk";
    private static final String COLLECTION_ID = "123";

    @Mock
    private PermissionsStore permissionsStore;

    @Mock
    private UsersService usersService;

    @Mock
    private TeamsService teamsService;

    @Mock
    private AccessMapping accessMapping;

    @Mock
    private User userMock;

    @Mock
    private Team teamMock;

    @Mock
    private Collection collectionMock;

    @Mock
    private CollectionDescription collectionDescription;

    private PermissionsService permissions;
    private ServiceSupplier<TeamsService> teamsServiceSupplier;
    private Session session;
    private Set<String> digitalPublishingTeam;
    private Set<String> admins;
    private List<Team> teamsList;
    private UserList userList;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        teamsList = new ArrayList<>();
        teamsList.add(teamMock);

        userList = new UserList();
        userList.add(userMock);

        teamsServiceSupplier = () -> teamsService;

        session = new Session();
        session.setEmail(EMAIL);

        digitalPublishingTeam = new HashSet<>();
        admins = new HashSet<>();

        when(userMock.getEmail())
                .thenReturn(EMAIL);

        permissions = new PermissionsServiceImpl(permissionsStore, teamsServiceSupplier);
    }

    @Test
    public void isPublisherBySession_ShouldReturnFalseIfSessionNull() throws Exception {
        session = null;

        assertThat(permissions.isPublisher(session), is(false));
        verifyNoInteractions(permissionsStore, usersService, teamsService);
    }

    @Test
    public void isPublisherBySession_ShouldReturnFalseIfSessionEmailNull() throws Exception {
        session.setEmail(null);

        assertThat(permissions.isPublisher(session), is(false));
        verifyNoInteractions(permissionsStore, usersService, teamsService);
    }

    @Test
    public void isPublisherBySession_ShouldReturnFalseIfSessionEmailEmpty() throws Exception {
        session.setEmail("");

        assertThat(permissions.isPublisher(session), is(false));
        verifyNoInteractions(permissionsStore, usersService, teamsService);
    }

    @Test
    public void isPublisherBySession_ShouldReturnFalseIfPSTIsNull() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getDigitalPublishingTeam())
                .thenReturn(null);

        assertThat(permissions.isPublisher(session), is(false));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(1)).getDigitalPublishingTeam();
        verifyNoInteractions(usersService, teamsService);
    }

    @Test
    public void isPublisherBySession_ShouldReturnFalseIfPSTDoesNotContainSessionEmail() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getDigitalPublishingTeam())
                .thenReturn(digitalPublishingTeam);

        assertThat(permissions.isPublisher(session), is(false));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(2)).getDigitalPublishingTeam();
        verifyNoInteractions(usersService, teamsService);
    }

    @Test
    public void isPublisherBySession_ShouldReturnTrueIfPSTContainsSessionEmail() throws Exception {
        digitalPublishingTeam.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getDigitalPublishingTeam())
                .thenReturn(digitalPublishingTeam);

        assertThat(permissions.isPublisher(session), is(true));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(2)).getDigitalPublishingTeam();
        verifyNoInteractions(usersService, teamsService);
    }

    @Test
    public void isAdministratorBySession_ShouldReturnFalseIsSessionNull() throws Exception {
        session = null;
        assertThat(permissions.isAdministrator(session), is(false));
        verifyNoInteractions(permissionsStore, usersService, accessMapping, teamsService);
    }

    @Test
    public void isAdministratorBySession_ShouldReturnFalseIsSessionEmailNull() throws Exception {
        session.setEmail(null);
        assertThat(permissions.isAdministrator(session), is(false));
        verifyNoInteractions(permissionsStore, usersService, accessMapping, teamsService);
    }

    @Test
    public void isAdministratorBySession_ShouldReturnFalseIfAdminsIsNull() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(null);

        assertThat(permissions.isAdministrator(session), is(false));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(1)).getAdministrators();
        verifyNoInteractions(usersService, teamsService);
    }

    @Test
    public void isAdministratorBySession_ShouldReturnFalseIfAdminsDoesNotContainEmail() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        assertThat(permissions.isAdministrator(session), is(false));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(2)).getAdministrators();
        verifyNoInteractions(usersService, teamsService);
    }

    @Test
    public void isAdministratorBySession_ShouldReturnTrueIfAdminsContainsEmail() throws Exception {
        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        assertThat(permissions.isAdministrator(session), is(true));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(2)).getAdministrators();
        verifyNoInteractions(usersService, teamsService);
    }

    @Test
    public void hasAdministrator_ShouldReturnFalseIfAdminsIsNull() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(null);

        assertThat(permissions.hasAdministrator(), is(false));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(1)).getAdministrators();
        verifyNoInteractions(usersService, teamsService);
    }

    @Test
    public void hasAdministrator_ShouldReturnFalseIfAdminsIsEmpty() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        assertThat(permissions.hasAdministrator(), is(false));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(2)).getAdministrators();
        verifyNoInteractions(usersService, teamsService);
    }

    @Test
    public void hasAdministrator_ShouldReturnTrueIfAdminsIsNotEmpty() throws Exception {
        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        assertThat(permissions.hasAdministrator(), is(true));
        verify(permissionsStore, times(1)).getAccessMapping();
        verify(accessMapping, times(2)).getAdministrators();
        verifyNoInteractions(usersService, teamsService);
    }


    @Test(expected = UnauthorizedException.class)
    public void removeAdministrator_ShouldThrowExceptionIfSessionNull() throws Exception {
        try {
            permissions.removeAdministrator(EMAIL, null);
        } catch (UnauthorizedException e) {
            verifyNoInteractions(permissionsStore, accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void removeAdministrator_ShouldThrowExceptionIfEmailNull() throws Exception {
        try {
            permissions.removeAdministrator(null, session);
        } catch (UnauthorizedException e) {
            verifyNoInteractions(permissionsStore, accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void removeAdministrator_ShouldThrowExceptionIfUserIsNotAnAdmin() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        try {
            permissions.removeAdministrator(EMAIL, session);
        } catch (UnauthorizedException e) {
            verify(permissionsStore, times(1)).getAccessMapping();
            verify(accessMapping, times(2)).getAdministrators();
            verifyNoMoreInteractions(accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test
    public void removeAdministrator_Success() throws Exception {
        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        permissions.removeAdministrator(EMAIL, session);
        verify(permissionsStore, times(2)).getAccessMapping();
        verify(accessMapping, times(4)).getAdministrators();
        verify(permissionsStore, times(1)).saveAccessMapping(accessMapping);
        verifyNoInteractions(usersService, teamsService);
    }


    @Test(expected = UnauthorizedException.class)
    public void removeEditor_ShouldThrowExceptionIfSessionNull() throws Exception {
        try {
            permissions.removeEditor(EMAIL, null);
        } catch (UnauthorizedException e) {
            verifyNoInteractions(permissionsStore, accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void removeEditor_ShouldThrowExceptionIfEmailNull() throws Exception {
        try {
            permissions.removeEditor(null, session);
        } catch (UnauthorizedException e) {
            verifyNoInteractions(permissionsStore, accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void removeEditor_ShouldThrowExceptionIfUserIsNotAnAdmin() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getDigitalPublishingTeam())
                .thenReturn(digitalPublishingTeam);

        try {
            permissions.removeEditor(EMAIL, session);
        } catch (UnauthorizedException e) {
            verify(permissionsStore, times(1)).getAccessMapping();
            verify(accessMapping, times(2)).getAdministrators();
            verifyNoMoreInteractions(accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test
    public void removeEditor_Success() throws Exception {
        digitalPublishingTeam.add(EMAIL);
        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getDigitalPublishingTeam())
                .thenReturn(digitalPublishingTeam);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        permissions.removeEditor(EMAIL, session);
        verify(permissionsStore, times(2)).getAccessMapping();
        verify(accessMapping, times(2)).getAdministrators();
        verify(permissionsStore, times(1)).saveAccessMapping(accessMapping);
        verifyNoInteractions(usersService, teamsService);
    }

    @Test(expected = UnauthorizedException.class)
    public void addAdministrator_ShouldThrowErrorSessionNull() throws Exception {
        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        try {
            permissions.addAdministrator(EMAIL, null);
        } catch (UnauthorizedException e) {
            verify(permissionsStore, times(1)).getAccessMapping();
            verify(accessMapping, times(2)).getAdministrators();
            verifyNoMoreInteractions(permissionsStore, accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void addAdministrator_ShouldThrowErrorSessionEmailNull() throws Exception {
        admins.add(EMAIL);
        session.setEmail(null);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        try {
            permissions.addAdministrator(EMAIL, session);
        } catch (UnauthorizedException e) {
            verify(permissionsStore, times(1)).getAccessMapping();
            verify(accessMapping, times(2)).getAdministrators();
            verifyNoMoreInteractions(permissionsStore, accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void addAdministrator_ShouldThrowErrorIfUserNotAdmin() throws Exception {
        admins.add(EMAIL);
        session.setEmail("test2@ons.gov.uk");

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        try {
            permissions.addAdministrator(EMAIL, session);
        } catch (UnauthorizedException e) {
            verify(permissionsStore, times(2)).getAccessMapping();
            verify(accessMapping, times(4)).getAdministrators();
            verifyNoMoreInteractions(permissionsStore, accessMapping, usersService, teamsService);
            throw e;
        }
    }

    @Test
    public void addAdministrator_Success() throws Exception {
        admins.add(EMAIL);
        String email2 = "test2@ons.gov.uk";

        Set<String> adminsMock = mock(Set.class);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins)
                .thenReturn(admins)
                .thenReturn(admins)
                .thenReturn(admins)
                .thenReturn(admins)
                .thenReturn(adminsMock);

        permissions.addAdministrator(email2, session);

        verify(permissionsStore, times(3)).getAccessMapping();
        verify(accessMapping, times(6)).getAdministrators();
        verify(permissionsStore, times(1)).saveAccessMapping(accessMapping);
        verify(adminsMock, times(1)).add(email2);
    }

    @Test
    public void canView_ShouldReturnFalseIfCollectionTeamsNull() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());
        when(teamsService.listTeams())
                .thenReturn(teamsList);

        assertThat(permissions.canView(session, COLLECTION_ID), is(false));

        verify(permissionsStore, times(1)).getAccessMapping();
        verifyNoInteractions(teamsService);
    }
}
