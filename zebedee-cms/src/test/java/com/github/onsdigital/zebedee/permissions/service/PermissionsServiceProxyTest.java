package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.UserList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PermissionsServiceProxyTest {

    private static final String EMAIL = "admin@ons.gov.uk";
    private static final String COLLECTION_ID = "1234";
    
    @Mock
    private PermissionsService legacyPermissionsService;

    @Mock
    private PermissionsService jwtPermissionsService;

    @Mock
    private Session session;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void isPublisher_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.isPublisher(session), is(false));
        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).isPublisher(session);
    }

    @Test
    public void isPublisher_JWEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.isPublisher(session), is(false));
        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).isPublisher(session);
    }

    @Test
    public void isAdministrator_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.isAdministrator(session), is(false));
        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).isAdministrator(session);
    }

    @Test
    public void isAdministrator_JWEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.isAdministrator(session), is(false));
        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).isAdministrator(session);
    }

    @Test
    public void hasAdministrator_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.hasAdministrator(), is(false));
        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).hasAdministrator();
    }

    @Test
    public void hasAdministrator_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.hasAdministrator(), is(false));
        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).hasAdministrator();
    }

    @Test
    public void addAdministrator_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.addAdministrator(EMAIL, session);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).addAdministrator(EMAIL, session);

    }

    @Test
    public void addAdministrator_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.addAdministrator(EMAIL, session);

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).addAdministrator(EMAIL, session);
    }


    @Test
    public void removeAdministrator_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.removeAdministrator(EMAIL, session);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).removeAdministrator(EMAIL, session);
    }

    @Test
    public void removeAdministrator_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.removeAdministrator(EMAIL, session);

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).removeAdministrator(EMAIL, session);
    }

    @Test
    public void canEdit_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.canEdit(session), is(false));

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).canEdit(session);
    }

    @Test
    public void canEdit_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.canEdit(session), is(false));

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).canEdit(session);
    }

    @Test
    public void addEditor_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.addEditor(EMAIL, session);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).addEditor(EMAIL, session);
    }

    @Test
    public void addEditor_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.addEditor(EMAIL, session);

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).addEditor(EMAIL, session);

    }

    @Test
    public void removeEditor_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.removeEditor(EMAIL, session);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).removeEditor(EMAIL, session);
    }

    @Test
    public void removeEditor_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.removeEditor(EMAIL, session);

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).removeEditor(EMAIL, session);
    }

    @Test
    public void canView_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.canView(session, COLLECTION_ID), is(false));

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).canView(session, COLLECTION_ID);
    }

    @Test
    public void canView_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        assertThat(permissions.canView(session, COLLECTION_ID), is(false));

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).canView(session, COLLECTION_ID);
    }

    @Test
    public void listViewerTeams_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.listViewerTeams(session, COLLECTION_ID);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).listViewerTeams(session, COLLECTION_ID);
    }

    @Test
    public void listViewerTeams_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.listViewerTeams(session, COLLECTION_ID);

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).listViewerTeams(session, COLLECTION_ID);
    }


    @Test
    public void setViewerTeams_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.setViewerTeams(session, COLLECTION_ID, new HashSet<>());

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).setViewerTeams(session, COLLECTION_ID, new HashSet<>());
    }

    @Test
    public void setViewerTeams_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.setViewerTeams(session, COLLECTION_ID, new HashSet<>());

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).setViewerTeams(session, COLLECTION_ID, new HashSet<>());
    }

    @Test
    public void userPermissions_Self_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.userPermissions(session);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).userPermissions(session);
    }

    @Test
    public void userPermissions_Self_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.userPermissions(session);

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).userPermissions(session);
    }

    @Test
    public void userPermissions_AnotherUser_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.userPermissions(session);

        verifyZeroInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).userPermissions(session);
    }

    @Test
    public void userPermissions_AnotherUser_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(jwtSessionsEnabled, legacyPermissionsService, jwtPermissionsService);

        permissions.userPermissions(EMAIL, session);

        verifyZeroInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).userPermissions(EMAIL, session);
    }
}

