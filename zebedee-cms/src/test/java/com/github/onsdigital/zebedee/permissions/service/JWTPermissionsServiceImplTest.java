package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class JWTPermissionsServiceImplTest {

    private static final String[] GROUP_0 = new String[]{"123456", "role-publisher", "role-admin", "789012345", "testgroup0"};
    private static final String[] GROUP_0A = new String[]{"123456", "role-publisher", "testgroup1"};
    private static final String[] GROUP_0B = new String[]{"123456", "role-admin", "testgroup1"};
    private static final String[] GROUP_0c = new String[]{"123456", "789012345"};
    private static final String[] GROUP_0d = new String[]{"testgroup1", "testgroup2"};
    private static final String COLLECTION_ID = "1234";
    private static final String FLORENCE_TOKEN = "666";
    private static final String TEST_USER_EMAIL = "other123@ons.gov.uk";
    private static final String TEST_SESSION_ID = "123test-session-id";
    private static final String PUBLISHER = "role-publisher";
    private static final String ADMIN = "role-admin";

    private JWTPermissionsServiceImpl jwtPermissionsService;

    @Mock
    private Session session;

    @Mock
    private CollectionDescription collectionDescriptionMock;

    @Mock
    private PermissionsStore jwtPermissionStore_Mock;

    @Mock
    private User user_Mock;

    @Mock
    private AccessMapping accessMapping_Mock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        jwtPermissionsService = new JWTPermissionsServiceImpl(jwtPermissionStore_Mock);

        session = new Session();
        session.setEmail("test@ons.gov.co.uk");
        session.setId("666");
        session.setLastAccess(null);
        session.setStart(null);

        when(jwtPermissionStore_Mock.getAccessMapping()).thenReturn(accessMapping_Mock);
    }

    @Test
    public void isPublisher_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.isPublisher(session));
    }

    @Test
    public void isPublisher_SessionNull_ShouldReturnFalse() throws Exception {
        Session session = null;
        assertFalse(jwtPermissionsService.isPublisher(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void isPublisher_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(null);
        session.setGroups(GROUP_0);
        assertFalse(jwtPermissionsService.isPublisher(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void isPublisher_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0c);
        assertFalse(jwtPermissionsService.isPublisher(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void isPublisher_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isPublisher(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void isAdministrator_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.isAdministrator(session));
    }

    @Test
    public void isAdministrator_SessionNull_ShouldReturnFalse() throws Exception {
        Session session = null;
        assertFalse(jwtPermissionsService.isAdministrator(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void isAdministrator_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        assertFalse(jwtPermissionsService.isAdministrator(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void isAdministrator_Session_NotAdmin_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0c);
        assertFalse(jwtPermissionsService.isAdministrator(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void isAdministrator_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isAdministrator(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void hasAdministrator_ShouldError() throws Exception {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                jwtPermissionsService.hasAdministrator());
        assertEquals("JWT sessions are enabled: hasAdministrator is no longer supported", exception.getMessage());
    }

    @Test
    public void addAdministrator_Email_Sessions_ShouldError() throws Exception {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                jwtPermissionsService.addAdministrator(TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: addAdministrator is no longer supported", exception.getMessage());
    }

    @Test
    public void removeAdministrator_Email_Sessions_ShouldError() throws Exception {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                jwtPermissionsService.removeAdministrator(TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: removeAdministrator is no longer supported", exception.getMessage());
    }

    @Test
    public void canEdit_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.canEdit(session));
    }

    @Test
    public void canEdit_SessionNull_ShouldReturnFalse() throws Exception {
        Session session = null;
        assertFalse(jwtPermissionsService.canEdit(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void canEdit_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(null);
        session.setGroups(GROUP_0c);
        assertFalse(jwtPermissionsService.canEdit(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void canEdit_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0c);
        assertFalse(jwtPermissionsService.canEdit(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void canEdit_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.canEdit(session));
        verifyZeroInteractions(jwtPermissionStore_Mock);
    }

    @Test
    public void addEditor_Email_Session_ShouldError() throws Exception {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                jwtPermissionsService.addEditor(TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: addEditor is no longer supported", exception.getMessage());
    }

    @Test
    public void removeEditor_Email_Session_ShouldError() throws Exception {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                jwtPermissionsService.removeEditor(TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: removeEditor is no longer supported", exception.getMessage());
    }

    @Test
    public void canView_CollectionId_Session() throws Exception {
        Integer teamId = 123456;
        String[] teamList = {String.valueOf(teamId)};

        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(teamList);
        session.setId(FLORENCE_TOKEN);

        Set<Integer> teams = new HashSet<Integer>() {{
            add(teamId);
        }};
        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put(COLLECTION_ID, teams);

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);

        assertTrue(jwtPermissionsService.canView(session, COLLECTION_ID));
    }

    @Test
    public void canView_Session_Null_CollectionId() throws IOException {
        assertFalse(jwtPermissionsService.canView(null, COLLECTION_ID));
    }

    @Test
    public void canView_Session_GroupNull_CollectionId() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setId(FLORENCE_TOKEN);

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put(COLLECTION_ID, new HashSet<>());

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);

        assertFalse(jwtPermissionsService.canView(session, COLLECTION_ID));
    }

    @Test
    public void canView_Session_CollectionId_Null() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        assertFalse(jwtPermissionsService.canView(session, null));
    }

    @Test
    public void canView_Session_CollectionDescription_NoPermissions() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        assertFalse(jwtPermissionsService.canView(session, COLLECTION_ID));
    }

    @Test
    public void setViewerTeams_Session_CollectionId_TeamIds() throws Exception {

        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        Set<Integer> teamList = new HashSet<Integer>() {{
            add(123456);
            add(789012345);
        }};

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);

        jwtPermissionsService.setViewerTeams(session, COLLECTION_ID, teamList);

        assertTrue(collectionMapping.get(COLLECTION_ID).size() == teamList.size());
        assertEquals(collectionMapping.get(COLLECTION_ID), teamList);
    }

    @Test
    public void setViewerTeams_Session_Null_CollectionId_TeamIds() throws Exception {
        Session session = new Session();
        
        Exception exception = assertThrows(UnauthorizedException.class, () ->
                jwtPermissionsService.setViewerTeams(session, COLLECTION_ID, new HashSet<>()));
        assertEquals("You do not have the right permission: null (null)", exception.getMessage());
    }

    @Test
    public void setViewerTeams_Session_CollectionId_TeamsList_emptyCollectionMapping() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);
        
        Integer teamId = 666;
        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        Set<Integer> teamList = new HashSet<>();
        teamList.add(teamId);

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);

        jwtPermissionsService.setViewerTeams(session, COLLECTION_ID, teamList);
        
        assertThat(collectionMapping, IsMapContaining.hasKey(COLLECTION_ID));
        assertTrue(collectionMapping.get(COLLECTION_ID).size() == teamList.size());
        assertTrue(collectionMapping.get(COLLECTION_ID).contains(teamId));
    }

    @Test
    public void listViewerTeams_collectionDescription_session() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId(COLLECTION_ID);
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        Set<Integer> teamList = new HashSet<Integer>() {{
            add(123456);
            add(789012345);
        }};
        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put(COLLECTION_ID, teamList);

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);

        Set<Integer> actual = jwtPermissionsService.listViewerTeams(session, COLLECTION_ID);
        assertThat(actual, is(notNullValue()));
        assertFalse(actual.isEmpty());
        assertTrue(actual.stream().anyMatch(c -> c.equals(789012345)));
        assertTrue(actual.stream().anyMatch(c -> c.equals(123456)));
        assertFalse(actual.stream().anyMatch(c -> c.equals(666)));
    }

    @Test
    public void listViewerTeams_session_collectionId_noPermissions() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put(COLLECTION_ID, new HashSet<>());

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);

        Exception exception = assertThrows(UnauthorizedException.class, () ->
                jwtPermissionsService.listViewerTeams(session, COLLECTION_ID));
        assertEquals("You do not have the right permission: other123@ons.gov.uk (666)", exception.getMessage());
    }

    @Test
    public void listViewerTeams_Session_Null_CollectionId() throws Exception {
        Exception exception = assertThrows(UnauthorizedException.class, () ->
                jwtPermissionsService.listViewerTeams(null, COLLECTION_ID));
        assertEquals("Please log in", exception.getMessage());
    }

    @Test
    public void setViewerTeams_CollectionDescription_Team_Session_collectionTeam() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        Integer teamId = 666;

        Set<Integer> originalList = new HashSet<Integer>() {{
            add(123456);
            add(789012345);
            add(teamId);
        }};
        Set<Integer> updatedList = new HashSet<Integer>() {{
                add(123456);
                add(789012345);
        }};

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put(COLLECTION_ID, originalList);

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);

        jwtPermissionsService.setViewerTeams(session, COLLECTION_ID, updatedList);

        assertThat(collectionMapping, is(notNullValue()));
        assertTrue(collectionMapping.get(COLLECTION_ID).size() > 0);
        assertTrue(collectionMapping.get(COLLECTION_ID).contains(789012345));
        assertTrue(collectionMapping.get(COLLECTION_ID).contains(123456));
        assertFalse(collectionMapping.get(COLLECTION_ID).contains(teamId));
    }

    @Test
    public void userPermissions_Email_Sessions_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationException.class, () ->
                jwtPermissionsService.userPermissions(TEST_USER_EMAIL, session));
        assertEquals("JWT sessions are enabled: userPermissions is no longer supported", exception.getMessage());
    }
}
