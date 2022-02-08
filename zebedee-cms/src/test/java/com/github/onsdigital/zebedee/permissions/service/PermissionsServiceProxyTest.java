package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void isPublisher_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.isPublisher(session), is(false));
        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).isPublisher(session);
    }

    @Test
    public void isPublisher_JWEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.isPublisher(session), is(false));
        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).isPublisher(session);
    }

    @Test
    public void isAdministrator_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.isAdministrator(session), is(false));
        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).isAdministrator(session);
    }

    @Test
    public void isAdministrator_JWEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.isAdministrator(session), is(false));
        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).isAdministrator(session);
    }

    @Test
    public void hasAdministrator_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.hasAdministrator(), is(false));
        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).hasAdministrator();
    }

    @Test
    public void hasAdministrator_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.hasAdministrator(), is(false));
        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).hasAdministrator();
    }

    @Test
    public void addAdministrator_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.addAdministrator(EMAIL, session);

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).addAdministrator(EMAIL, session);

    }

    @Test
    public void addAdministrator_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.addAdministrator(EMAIL, session);

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).addAdministrator(EMAIL, session);
    }


    @Test
    public void removeAdministrator_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.removeAdministrator(EMAIL, session);

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).removeAdministrator(EMAIL, session);
    }

    @Test
    public void removeAdministrator_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.removeAdministrator(EMAIL, session);

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).removeAdministrator(EMAIL, session);
    }

    @Test
    public void canEdit_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.canEdit(session), is(false));

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).canEdit(session);
    }

    @Test
    public void canEdit_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.canEdit(session), is(false));

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).canEdit(session);
    }

    @Test
    public void addEditor_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.addEditor(EMAIL, session);

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).addEditor(EMAIL, session);
    }

    @Test
    public void addEditor_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.addEditor(EMAIL, session);

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).addEditor(EMAIL, session);

    }

    @Test
    public void removeEditor_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.removeEditor(EMAIL, session);

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).removeEditor(EMAIL, session);
    }

    @Test
    public void removeEditor_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.removeEditor(EMAIL, session);

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).removeEditor(EMAIL, session);
    }

    @Test
    public void canView_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.canView(session, COLLECTION_ID), is(false));

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).canView(session, COLLECTION_ID);
    }

    @Test
    public void canView_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        assertThat(permissions.canView(session, COLLECTION_ID), is(false));

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).canView(session, COLLECTION_ID);
    }

    @Test
    public void listViewerTeams_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.listViewerTeams(session, COLLECTION_ID);

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).listViewerTeams(session, COLLECTION_ID);
    }

    @Test
    public void listViewerTeams_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.listViewerTeams(session, COLLECTION_ID);

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).listViewerTeams(session, COLLECTION_ID);
    }


    @Test
    public void setViewerTeams_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.setViewerTeams(session, COLLECTION_ID, new HashSet<>());

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).setViewerTeams(session, COLLECTION_ID, new HashSet<>());
    }

    @Test
    public void setViewerTeams_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.setViewerTeams(session, COLLECTION_ID, new HashSet<>());

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).setViewerTeams(session, COLLECTION_ID, new HashSet<>());
    }

    @Test
    public void userPermissions_Self_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.userPermissions(session);

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).userPermissions(session);
    }

    @Test
    public void userPermissions_Self_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.userPermissions(session);

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).userPermissions(session);
    }

    @Test
    public void userPermissions_AnotherUser_JWTNotEnabled() throws Exception {
        boolean jwtSessionsEnabled = false;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.userPermissions(session);

        verifyNoInteractions(jwtPermissionsService);
        verify(legacyPermissionsService, atLeastOnce()).userPermissions(session);
    }

    @Test
    public void userPermissions_AnotherUser_JWTEnabled() throws Exception {
        boolean jwtSessionsEnabled = true;
        PermissionsServiceProxy permissions = new PermissionsServiceProxy(legacyPermissionsService, jwtPermissionsService, jwtSessionsEnabled);

        permissions.userPermissions(EMAIL, session);

        verifyNoInteractions(legacyPermissionsService);
        verify(jwtPermissionsService, atLeastOnce()).userPermissions(EMAIL, session);
    }
}

