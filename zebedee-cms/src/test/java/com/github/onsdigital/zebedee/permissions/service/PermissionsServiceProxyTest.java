package com.github.onsdigital.zebedee.permissions.service;

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

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

/**
 * Created by dave on 31/05/2017.
 */
public class PermissionsServiceProxyTest {

    private static final String EMAIL = "admin@ons.gov.uk";

    @Mock
    private PermissionsStore permissionsStore;

    @Mock
    private PermissionsService legacyPermissionsService;

    @Mock
    private PermissionsService jwtPermissionsService;

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
    private ServiceSupplier<UsersService> usersServiceSupplier;
    private ServiceSupplier<TeamsService> teamsServiceSupplier;
    private Session session;
    private Set<String> digitalPublishingTeam;
    private Set<String> admins;
    private List<Team> teamsList;
    private UserList userList;
    private PermissionsService permissions;
    private Boolean jwtSessionsEnabled;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        teamsList = new ArrayList<>();
        teamsList.add(teamMock);

        userList = new UserList();
        userList.add(userMock);

        ServiceSupplier<UsersService> usersServiceSupplier;
        usersServiceSupplier = () -> usersService;
        teamsServiceSupplier = () -> teamsService;

        session = new Session();
        session.setEmail(EMAIL);

        digitalPublishingTeam = new HashSet<>();
        admins = new HashSet<>();

        when(userMock.getEmail())
                .thenReturn(EMAIL);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_Session_JWTNotEnabled() throws Exception {
        session = null;
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        assertThat(permissions.isPublisher(session), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).isPublisher(session);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_Session_JWEnabled() throws Exception {
        session = null;
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        assertThat(permissions.isPublisher(session), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).isPublisher(session);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_Email_JWTNotEnabled() throws Exception {
        String email = null;
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        assertThat(permissions.isPublisher(email), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).isPublisher(email);

    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_Email_JWEnabled() throws Exception {
        String email = null;
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        assertThat(permissions.isPublisher(email), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).isPublisher(email);

    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Session_JWTNotEnabled() throws Exception {
        session = null;
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        assertThat(permissions.isAdministrator(session), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).isAdministrator(session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Session_JWEnabled() throws Exception {
        session = null;
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        assertThat(permissions.isAdministrator(session), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).isAdministrator(session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Email_JWTNotEnabled() throws Exception {
        String email = null;
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        assertThat(permissions.isAdministrator(email), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).isAdministrator(email);

    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Email_JWEnabled() throws Exception {
        String email = null;
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        assertThat(permissions.isAdministrator(email), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).isAdministrator(email);

    }


    /**
     * @throws Exception
     */
    @Test
    public void hasAdministrator_JWTNotEnabled() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(null);

        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.hasAdministrator(), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).hasAdministrator();
    }

    /**
     * @throws Exception
     */
    @Test
    public void hasAdministrator_JWTEnabled() throws Exception {
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(null);

        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.hasAdministrator(), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).hasAdministrator();
    }


    /**
     * @throws Exception
     */
    @Test
    public void addAdministrator_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        admins.add(EMAIL);
        String email2 = "test2@ons.gov.uk";
        permissions.addAdministrator(email2, session);
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).addAdministrator(email2, session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void addAdministrator_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        admins.add(EMAIL);
        String email2 = "test2@ons.gov.uk";
        permissions.addAdministrator(email2, session);
        verifyZeroInteractions(usersService, teamsService);
        verify(jwtPermissionsService, atLeastOnce()).addAdministrator(email2, session);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canView_User_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());
        assertThat(permissions.canView(userMock, collectionDescription), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).canView(userMock, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canView_User_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());

        assertThat(permissions.canView(userMock, collectionDescription), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).canView(userMock, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canView_Sessions_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        session = null;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());


        assertThat(permissions.canView(session, collectionDescription), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).canView(session, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canView_Sessions_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        session = null;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());

        assertThat(permissions.canView(session, collectionDescription), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).canView(session, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canView_Email_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        String email = null;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());
        assertThat(permissions.canView(email, collectionDescription), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, atLeastOnce()).canView(email, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canView_Email_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        String email = null;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());
        assertThat(permissions.canView(email, collectionDescription), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, atLeastOnce()).canView(email, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canEdit_Session_JWTNotEnabled() throws Exception {
        session = null;
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());


        assertThat(permissions.canView(session, collectionDescription), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).canView(session, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canEdit_Session_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        session = null;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());

        assertThat(permissions.canView(userMock, collectionDescription), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).canView(userMock, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canEdit_User_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());


        assertThat(permissions.canView(userMock, collectionDescription), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).canView(userMock, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canEdit_User_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());

        assertThat(permissions.canView(userMock, collectionDescription), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).canView(userMock, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canEdit_Email_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        String email = null;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());


        assertThat(permissions.canView(email, collectionDescription), is(false));
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).canView(email, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void canEdit_Email_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        String email = null;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        when(permissionsStore.getAccessMapping())
                .thenReturn(new AccessMapping());

        assertThat(permissions.canView(email, collectionDescription), is(false));
        verifyZeroInteractions(legacyPermissionsService, usersService, teamsService);
        verify(jwtPermissionsService, times(1)).canView(email, collectionDescription);
    }

    /**
     * @throws Exception
     */
    @Test
    public void listCollectionsAccessibleByTeam_JWTNotEnabled() throws Exception {

        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put("1234", new HashSet<Integer>() {{
            add(1);
            add(2);
        }});
        collectionMapping.put("5678", new HashSet<Integer>() {{
            add(2);
            add(3);
        }});

        when(accessMapping.getCollections())
                .thenReturn(collectionMapping);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);

        Team t = new Team();
        t.setId(6);

        Set<String> actual = permissions.listCollectionsAccessibleByTeam(t);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, times(1)).listCollectionsAccessibleByTeam(t);
    }

    /**
     * @throws Exception
     */
    @Test
    public void removeEditor_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        digitalPublishingTeam.add(EMAIL);
        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getDigitalPublishingTeam())
                .thenReturn(digitalPublishingTeam);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        permissions.removeEditor(EMAIL, session);
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).removeEditor(EMAIL, session);
    }

    /**
     * @throws Exception
     */
    @Test
    public void removeEditor_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        digitalPublishingTeam.add(EMAIL);
        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getDigitalPublishingTeam())
                .thenReturn(digitalPublishingTeam);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        permissions.removeEditor(EMAIL, session);
        verifyZeroInteractions(usersService, teamsService);
        verify(jwtPermissionsService, times(1)).removeEditor(EMAIL, session);
    }

    /**
     * @throws Exception
     */
    @Test
    public void removeAdministrator_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        permissions.removeAdministrator(EMAIL, session);
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).removeAdministrator(EMAIL, session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void removeAdministrator_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        admins.add(EMAIL);

        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getAdministrators())
                .thenReturn(admins);

        permissions.removeAdministrator(EMAIL, session);
        verifyZeroInteractions(usersService, teamsService);
        verify(jwtPermissionsService, times(1)).removeAdministrator(EMAIL, session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void listViewerTeams_JWTNotEnabled() throws Exception {

        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        Team t = new Team();
        t.setId(6);
        session = null;

        Set<String> actual = permissions.listCollectionsAccessibleByTeam(t);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, times(1)).listCollectionsAccessibleByTeam(t);
    }


    /**
     * @throws Exception
     */
    @Test
    public void userPermissions_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        admins.add(EMAIL);
        String email2 = "test2@ons.gov.uk";

        permissions.userPermissions(email2, session);
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).userPermissions(email2, session);

    }


    /**
     * @throws Exception
     */
    @Test
    public void addEditor_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        String email2 = "test2@ons.gov.uk";
        session = null;


        permissions.addEditor(email2, session);
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).addEditor(email2, session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void addEditor_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        String email2 = "test2@ons.gov.uk";
        session = null;

        permissions.addEditor(email2, session);
        verifyZeroInteractions(usersService, teamsService);
        verify(jwtPermissionsService, times(1)).addEditor(email2, session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void addViewerTeam_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        session = null;
        Integer t = 6;

        permissions.addViewerTeam(collectionDescription, t, session);
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).addViewerTeam(collectionDescription, t, session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void addViewerTeam_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        session = null;
        Integer t = 6;
        permissions.addViewerTeam(collectionDescription, t, session);
        verifyZeroInteractions(usersService, teamsService);
        verify(jwtPermissionsService, times(1)).addViewerTeam(collectionDescription, t, session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void removeViewerTeam_JWTNotEnabled() throws Exception {
        jwtSessionsEnabled = false;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        session = null;
        Integer t = 6;

        permissions.removeViewerTeam(collectionDescription, t, session);
        verifyZeroInteractions(jwtPermissionsService, usersService, teamsService);
        verify(legacyPermissionsService, times(1)).removeViewerTeam(collectionDescription, t, session);

    }

    /**
     * @throws Exception
     */
    @Test
    public void removeViewerTeam_JWTEnabled() throws Exception {
        jwtSessionsEnabled = true;
        permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);
        session = null;
        Integer t = 6;
        permissions.removeViewerTeam(collectionDescription, t, session);
        verifyZeroInteractions(usersService, teamsService);
        verify(jwtPermissionsService, times(1)).removeViewerTeam(collectionDescription, t, session);

    }

}

