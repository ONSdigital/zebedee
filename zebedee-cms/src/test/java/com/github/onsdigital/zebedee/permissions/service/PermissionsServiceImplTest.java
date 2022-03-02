package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import org.junit.After;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by dave on 31/05/2017.
 */
@SuppressWarnings("ALL")
public class PermissionsServiceImplTest {

    private static final String EMAIL = "admin@ons.gov.uk";
    private static final String COLLECTION_ID = "123";

    private final Set<String> digitalPublishingTeam = new HashSet<>();
    private final Set<String> admins = new HashSet<>();

    /**
     * Class under test
     */
    private PermissionsServiceImpl permissions;

    @Mock
    private PermissionsStore permissionsStore;

    @Mock
    private TeamsService teamsService;

    @Mock
    private AccessMapping accessMapping;

    @Mock
    private Team teamMock;

    @Mock
    private Session session;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        List<Team> teamsList = new ArrayList<>();
        teamsList.add(teamMock);

        when(teamsService.listTeams())
                .thenReturn(teamsList);
        when(session.getEmail())
                .thenReturn(EMAIL);

        permissions = new PermissionsServiceImpl(permissionsStore, () -> teamsService);
    }

    @After
    public void teardown() {
        digitalPublishingTeam.clear();
        admins.clear();
    }

    @Test
    public void isPublisherBySession_ShouldReturnFalseIfSessionNull() throws Exception {
        assertThat(permissions.isPublisher(null), is(false));
        verifyNoInteractions(permissionsStore, teamsService);
    }

    @Test
    public void isPublisherBySession_ShouldReturnFalseIfSessionEmailNull() throws Exception {
        when(session.getEmail())
                .thenReturn(null);

        assertThat(permissions.isPublisher(session), is(false));
        verifyNoInteractions(permissionsStore, teamsService);
    }

    @Test
    public void isPublisherBySession_ShouldReturnFalseIfSessionEmailEmpty() throws Exception {
        when(session.getEmail())
                .thenReturn("");

        assertThat(permissions.isPublisher(session), is(false));
        verifyNoInteractions(permissionsStore, teamsService);
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
        verifyNoInteractions(teamsService);
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
        verifyNoInteractions(teamsService);
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
        verifyNoInteractions(teamsService);
    }

    @Test
    public void isAdministratorBySession_ShouldReturnFalseIsSessionNull() throws Exception {
        assertThat(permissions.isAdministrator(null), is(false));
        verifyNoInteractions(permissionsStore, accessMapping, teamsService);
    }

    @Test
    public void isAdministratorBySession_ShouldReturnFalseIsSessionEmailNull() throws Exception {
        when(session.getEmail())
                .thenReturn(null);

        assertThat(permissions.isAdministrator(session), is(false));
        verifyNoInteractions(permissionsStore, accessMapping, teamsService);
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
        verifyNoInteractions(teamsService);
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
        verifyNoInteractions(teamsService);
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
        verifyNoInteractions(teamsService);
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
        verifyNoInteractions(teamsService);
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
        verifyNoInteractions(teamsService);
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
        verifyNoInteractions(teamsService);
    }


    @Test(expected = UnauthorizedException.class)
    public void removeAdministrator_ShouldThrowExceptionIfSessionNull() throws Exception {
        try {
            permissions.removeAdministrator(EMAIL, null);
        } catch (UnauthorizedException e) {
            verifyNoInteractions(permissionsStore, accessMapping, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void removeAdministrator_ShouldThrowExceptionIfEmailNull() throws Exception {
        try {
            permissions.removeAdministrator(null, session);
        } catch (UnauthorizedException e) {
            verifyNoInteractions(permissionsStore, accessMapping, teamsService);
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
            verifyNoMoreInteractions(accessMapping, teamsService);
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
        verifyNoInteractions(teamsService);
    }


    @Test(expected = UnauthorizedException.class)
    public void removeEditor_ShouldThrowExceptionIfSessionNull() throws Exception {
        try {
            permissions.removeEditor(EMAIL, null);
        } catch (UnauthorizedException e) {
            verifyNoInteractions(permissionsStore, accessMapping, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void removeEditor_ShouldThrowExceptionIfEmailNull() throws Exception {
        try {
            permissions.removeEditor(null, session);
        } catch (UnauthorizedException e) {
            verifyNoInteractions(permissionsStore, accessMapping, teamsService);
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
            verifyNoMoreInteractions(accessMapping, teamsService);
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
        verifyNoInteractions(teamsService);
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
            verifyNoMoreInteractions(permissionsStore, accessMapping, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void addAdministrator_ShouldThrowErrorSessionEmailNull() throws Exception {
        admins.add(EMAIL);

        when(session.getEmail())
                .thenReturn(null);
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        try {
            permissions.addAdministrator(EMAIL, session);
        } catch (UnauthorizedException e) {
            verify(permissionsStore, times(1)).getAccessMapping();
            verify(accessMapping, times(2)).getAdministrators();
            verifyNoMoreInteractions(permissionsStore, accessMapping, teamsService);
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void addAdministrator_ShouldThrowErrorIfUserNotAdmin() throws Exception {
        // Add an admin to bypass the hasAdministrator() shortcut that allows for the first admin to be created
        admins.add("someone@ons.gov.uk");
        String email2 = "test2@ons.gov.uk";

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        try {
            permissions.addAdministrator(email2, session);
        } catch (UnauthorizedException e) {
            verify(permissionsStore, times(2)).getAccessMapping();
            verify(accessMapping, times(4)).getAdministrators();
            verifyNoMoreInteractions(permissionsStore, accessMapping, teamsService);
            throw e;
        }
    }


    @Test
    public void addAdministrator_Success() throws Exception {
        admins.add(EMAIL);
        String email2 = "test2@ons.gov.uk";

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        permissions.addAdministrator(email2, session);

        assertEquals(2, admins.size());
        assertTrue(admins.contains(email2));
        verify(permissionsStore, times(3)).getAccessMapping();
        verify(accessMapping, times(6)).getAdministrators();
        verify(permissionsStore, times(1)).saveAccessMapping(accessMapping);
    }

    @Test
    public void addAdministrator_ShouldSucceed_WhenNoAdminExists() throws Exception {
        String email2 = "test2@ons.gov.uk";

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        permissions.addAdministrator(email2, null);

        assertEquals(1, admins.size());
        assertTrue(admins.contains(email2));
        verify(permissionsStore, times(2)).getAccessMapping();
        verify(accessMapping, times(4)).getAdministrators();
        verify(permissionsStore, times(1)).saveAccessMapping(accessMapping);
    }

    @Test
    public void canView_ShouldReturnFalseIfCollectionTeamsNull() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());

        assertThat(permissions.canView(session, COLLECTION_ID), is(false));

        verify(permissionsStore, times(1)).getAccessMapping();
        verifyNoInteractions(teamsService);
    }
}
