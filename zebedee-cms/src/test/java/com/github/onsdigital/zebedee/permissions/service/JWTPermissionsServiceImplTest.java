package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.session.model.Session;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class JWTPermissionsServiceImplTest {
    private static final List<String> ADMIN_PUBLISHER_GROUPS = Arrays.asList("123456", "role-publisher", "role-admin", "789012345", "testgroup0");
    private static final List<String> NON_ADMIN_PUBLISHER_GROUPS = Arrays.asList("123456", "789012345");
    private static final String COLLECTION_ID = "1234";
    private static final String TEST_SESSION_ID = "666";
    private static final String TEST_USER_EMAIL = "other123@ons.gov.uk";
    private static final String PUBLISHER = "role-publisher";
    private static final String ADMIN = "role-admin";

    /**
     * Class under test
     */
    private JWTPermissionsServiceImpl jwtPermissionsService;

    @Mock
    private PermissionsStore jwtPermissionStore;

    @Mock
    private AccessMapping accessMapping;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        this.jwtPermissionsService = new JWTPermissionsServiceImpl(this.jwtPermissionStore);

        when(this.jwtPermissionStore.getAccessMapping()).thenReturn(this.accessMapping);
    }

    @Test
    public void isPublisher_Session_Publisher_ShouldReturnTrue() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);

        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);
        assertTrue(this.jwtPermissionsService.isPublisher(session));
    }

    @Test
    public void isPublisher_Session_Admin_ShouldReturnFalse() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.ADMIN);

        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);
        assertFalse(this.jwtPermissionsService.isPublisher(session));
    }

    @Test
    public void isPublisher_SessionNull_ShouldReturnFalse() throws Exception {
        final Session session = null;
        assertFalse(this.jwtPermissionsService.isPublisher(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void isPublisher_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, new ArrayList<>());
        assertFalse(this.jwtPermissionsService.isPublisher(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void isPublisher_Session_NullGroup_ShouldReturnFalse() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, null);
        assertFalse(this.jwtPermissionsService.isPublisher(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void isAdministrator_Session_Admin_ShouldReturnTrue() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.ADMIN);

        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);
        assertTrue(this.jwtPermissionsService.isAdministrator(session));
    }

    @Test
    public void isAdministrator_Session_Publisher_ShouldReturnFalse() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);

        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);
        assertFalse(this.jwtPermissionsService.isAdministrator(session));
    }

    @Test
    public void isAdministrator_SessionNull_ShouldReturnFalse() throws Exception {
        final Session session = null;
        assertFalse(this.jwtPermissionsService.isAdministrator(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void isAdministrator_Session_EmailNull_ShouldReturnFalse() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);

        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, null, sessionGroups);
        assertFalse(this.jwtPermissionsService.isAdministrator(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void isAdministrator_Session_NotAdmin_ShouldReturnFalse() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, new ArrayList<>());
        assertFalse(this.jwtPermissionsService.isAdministrator(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void isAdministrator_Session_NullGroup_ShouldReturnFalse() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, null);
        assertFalse(this.jwtPermissionsService.isAdministrator(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void hasAdministrator_ShouldError() throws Exception {
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                this.jwtPermissionsService.hasAdministrator());
        assertEquals("JWT sessions are enabled: hasAdministrator is no longer supported", exception.getMessage());
    }

    @Test
    public void addAdministrator_Email_Sessions_ShouldError() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL);
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                this.jwtPermissionsService.addAdministrator(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: addAdministrator is no longer supported", exception.getMessage());
    }

    @Test
    public void removeAdministrator_Email_Sessions_ShouldError() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL);
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                this.jwtPermissionsService.removeAdministrator(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: removeAdministrator is no longer supported", exception.getMessage());
    }

    @Test
    public void canEdit_Session_Publisher_ShouldReturnTrue() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);

        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);
        assertTrue(this.jwtPermissionsService.canEdit(session));
    }

    @Test
    public void canEdit_SessionNull_ShouldReturnFalse() throws Exception {
        final Session session = null;
        assertFalse(this.jwtPermissionsService.canEdit(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void canEdit_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, new ArrayList<>());

        assertFalse(this.jwtPermissionsService.canEdit(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void canEdit_Session_NullGroup_ShouldReturnFalse() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, null);
        assertFalse(this.jwtPermissionsService.canEdit(session));
        verifyNoInteractions(this.jwtPermissionStore);
    }

    @Test
    public void addEditor_Email_Session_ShouldError() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL);
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                this.jwtPermissionsService.addEditor(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: addEditor is no longer supported", exception.getMessage());
    }

    @Test
    public void removeEditor_Email_Session_ShouldError() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL);
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                this.jwtPermissionsService.removeEditor(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: removeEditor is no longer supported", exception.getMessage());
    }

    @Test
    public void canView_CollectionId_Session_publisher() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);

        assertTrue(this.jwtPermissionsService.canView(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID));
    }

    @Test
    public void canView_CollectionId_Session_viewer() throws Exception {
        final String teamId = "123456";
        final List<String> teamList = new ArrayList<>();
        teamList.add(teamId);

        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, teamList);

        final Set<String> teams = new HashSet<String>() {{
            this.add("12345");
            this.add("67890");
            this.add(teamId);
        }};
        final Map<String, Set<String>> collectionMapping = new HashMap<>();
        collectionMapping.put(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, teams);

        when(this.accessMapping.getCollections()).thenReturn(collectionMapping);

        assertTrue(this.jwtPermissionsService.canView(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID));
    }

    @Test
    public void canView_Session_Null_CollectionId() throws Exception {
        assertFalse(this.jwtPermissionsService.canView(null, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID));
    }

    @Test
    public void canView_Session_GroupNull_CollectionId() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, null);

        final Map<String, Set<String>> collectionMapping = new HashMap<>();
        collectionMapping.put(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, new HashSet<>());

        when(this.accessMapping.getCollections()).thenReturn(collectionMapping);

        assertFalse(this.jwtPermissionsService.canView(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID));
    }

    @Test
    public void canView_Session_CollectionId_Null() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, new ArrayList<>());

        assertFalse(this.jwtPermissionsService.canView(session, null));
    }

    @Test
    public void canView_Session_CollectionDescription_NoPermissions() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add("7890");
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);

        final Set<String> teams = new HashSet<String>() {{
            this.add("12345");
            this.add("67890");
        }};
        final Map<String, Set<String>> collectionMapping = new HashMap<>();
        collectionMapping.put(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, teams);

        when(this.accessMapping.getCollections()).thenReturn(collectionMapping);

        assertFalse(this.jwtPermissionsService.canView(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID));
    }

    @Test
    public void setViewerTeams_Session_CollectionId_TeamIds() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);

        final Set<String> teamList = new HashSet<String>() {{
            this.add("123456");
            this.add("789012345");
        }};

        final Map<String, Set<String>> collectionMapping = new HashMap<>();

        when(this.accessMapping.getCollections()).thenReturn(collectionMapping);

        this.jwtPermissionsService.setViewerTeams(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, teamList);

        assertEquals(teamList.size(), collectionMapping.get(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID).size());
        assertEquals(collectionMapping.get(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID), teamList);
    }

    @Test
    public void setViewerTeams_Session_Null_CollectionId_TeamIds() throws Exception {
        final Session session = new Session(null, null, null);

        final Exception exception = assertThrows(UnauthorizedException.class, () ->
                this.jwtPermissionsService.setViewerTeams(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, new HashSet<>()));
        assertEquals("You do not have the right permission: null (null)", exception.getMessage());
    }

    @Test
    public void setViewerTeams_Session_CollectionId_TeamsList_emptyCollectionMapping() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);

        final String teamId = "666";
        final Map<String, Set<String>> collectionMapping = new HashMap<>();
        final Set<String> teamList = new HashSet<>();
        teamList.add(teamId);

        when(this.accessMapping.getCollections()).thenReturn(collectionMapping);

        this.jwtPermissionsService.setViewerTeams(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, teamList);

        assertThat(collectionMapping, IsMapContaining.hasKey(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID));
        assertEquals(teamList.size(), collectionMapping.get(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID).size());
        assertTrue(collectionMapping.get(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID).contains(teamId));
    }

    @Test
    public void listViewerTeams_collectionDescription_session() throws Exception {
        final List<String> sessionGroups = new ArrayList<>();
        sessionGroups.add(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER);
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, sessionGroups);

        final CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID);
        collectionDescriptionMock.setTeams(Arrays.asList(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.PUBLISHER));

        final Set<String> teamList = new HashSet<String>() {{
            this.add("123456");
            this.add("789012345");
        }};
        final Map<String, Set<String>> collectionMapping = new HashMap<>();
        collectionMapping.put(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, teamList);

        when(this.accessMapping.getCollections()).thenReturn(collectionMapping);

        final Set<String> actual = this.jwtPermissionsService.listViewerTeams(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID);
        assertThat(actual, is(notNullValue()));
        assertFalse(actual.isEmpty());
        assertTrue(actual.stream().anyMatch(c -> c.equals("789012345")));
        assertTrue(actual.stream().anyMatch(c -> c.equals("123456")));
        assertFalse(actual.stream().anyMatch(c -> c.equals("666")));
    }

    @Test
    public void listViewerTeams_session_collectionId_noPermissions() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, new ArrayList<>());

        final Map<String, Set<String>> collectionMapping = new HashMap<>();
        collectionMapping.put(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, new HashSet<>());

        when(this.accessMapping.getCollections()).thenReturn(collectionMapping);

        final Exception exception = assertThrows(UnauthorizedException.class, () ->
                this.jwtPermissionsService.listViewerTeams(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID));
        assertEquals("You do not have the right permission: other123@ons.gov.uk (666)", exception.getMessage());
    }

    @Test
    public void listViewerTeams_Session_Null_CollectionId() throws Exception {
        final Exception exception = assertThrows(UnauthorizedException.class, () ->
                this.jwtPermissionsService.listViewerTeams(null, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID));
        assertEquals("Please log in", exception.getMessage());
    }

    @Test
    public void setViewerTeams_CollectionDescription_Team_Session_collectionTeam() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.ADMIN_PUBLISHER_GROUPS);

        final String teamId = "666";

        final Set<String> originalList = new HashSet<String>() {{
            this.add("123456");
            this.add("789012345");
            this.add(teamId);
        }};
        final Set<String> updatedList = new HashSet<String>() {{
            this.add("123456");
            this.add("789012345");
        }};

        final Map<String, Set<String>> collectionMapping = new HashMap<>();
        collectionMapping.put(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, originalList);

        when(this.accessMapping.getCollections()).thenReturn(collectionMapping);

        this.jwtPermissionsService.setViewerTeams(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, updatedList);

        assertThat(collectionMapping, is(notNullValue()));
        assertTrue(collectionMapping.get(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID).size() > 0);
        assertTrue(collectionMapping.get(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID).contains("789012345"));
        assertTrue(collectionMapping.get(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID).contains("123456"));
        assertFalse(collectionMapping.get(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID).contains(teamId));
    }

    @Test
    public void setViewerTeams_CollectionDescription_Team_Session_viewer_collectionTeam() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, new ArrayList<>());

        final Exception exception = assertThrows(UnauthorizedException.class, () ->
                this.jwtPermissionsService.setViewerTeams(session, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.COLLECTION_ID, new HashSet<String>()));
    }

    @Test
    public void userPermissions_Email_Sessions_ShouldError() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL);
        final Exception exception = assertThrows(UnsupportedOperationException.class, () ->
                this.jwtPermissionsService.userPermissions(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: userPermissions is no longer supported", exception.getMessage());
    }

    @Test
    public void userPermissions_Sessions() throws Exception {
        final Session session = new Session(com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_SESSION_ID, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL, com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.ADMIN_PUBLISHER_GROUPS);
        final PermissionDefinition actual = this.jwtPermissionsService.userPermissions(session);
        assertTrue(actual.isAdmin());
        assertTrue(actual.isEditor());
        assertTrue(actual.getEmail() == com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImplTest.TEST_USER_EMAIL);
    }
}
