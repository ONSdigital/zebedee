package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.UnsupportedOperationExceptions;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JWTPermissionsServiceImplTest {

    private static final String[] GROUP_0 = new String[]{"123456", "role-publisher", "role-admin", "789012345", "testgroup0"};
    private static final String[] GROUP_0A = new String[]{"123456", "role-publisher", "testgroup1"};
    private static final String[] GROUP_0B = new String[]{"123456", "role-admin", "testgroup1"};
    private static final String[] GROUP_0c = new String[]{"123456", "789012345"};
    private static final String[] GROUP_0d = new String[]{"testgroup1", "testgroup2"};
    private static final String FLORENCE_TOKEN = "666";
    private static final String TEST_USER_EMAIL = "other123@ons.gov.uk";
    private static final String TEST_SESSION_ID = "123test-session-id";
    private static final String PUBLISHER = "role-publisher";
    private static final String ADMIN = "role-admin";
    private PermissionsService jwtPermissionsService;
    @Mock
    private Session session;

    @Mock
    private CollectionDescription collectionDescriptionMock;

    @Mock
    private JWTPermissionsServiceImpl jwtPSI_Mock;

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
    }

    @Test
    public void isGroupMember_Admin_ShouldTrue() {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        when(jwtPSI_Mock.isGroupMember(session, ADMIN)).thenReturn(true);
        assertTrue(jwtPSI_Mock.isGroupMember(session, ADMIN));
        verify(jwtPSI_Mock, atLeastOnce()).isGroupMember(session, ADMIN);
    }

    @Test
    public void isGroupMember_Publisher_ShouldTrue() {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        when(jwtPSI_Mock.isGroupMember(session, PUBLISHER)).thenReturn(true);
        assertTrue(jwtPSI_Mock.isGroupMember(session, PUBLISHER));
        verify(jwtPSI_Mock, atLeastOnce()).isGroupMember(session, PUBLISHER);
    }

    @Test
    public void isGroupMember_PublishNotInGroup_ShouldFalse() {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0B);
        when(jwtPSI_Mock.isGroupMember(session, PUBLISHER)).thenReturn(false);
        assertFalse(jwtPSI_Mock.isGroupMember(session, PUBLISHER));
    }

    @Test
    public void isGroupMember_AdminNotInGroup_ShouldFalse() {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0A);
        when(jwtPSI_Mock.isGroupMember(session, ADMIN)).thenReturn(false);
        assertFalse(jwtPSI_Mock.isGroupMember(session, PUBLISHER));
    }

    @Test
    public void isGroupMember_nullSession_ShouldFalse() {
        Session session = null;
        assertFalse(jwtPSI_Mock.isGroupMember(session, ADMIN));
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
    public void isPublisher_Email_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.isPublisher(TEST_USER_EMAIL));
        assertEquals("JWT Permissions service error for isPublisher no longer required", exception.getMessage());
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
    public void isAdministrator_Email_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.isAdministrator(TEST_USER_EMAIL));
        assertEquals("JWT Permissions service error for isAdministrator no longer required", exception.getMessage());
    }

    @Test
    public void hasAdministrator_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.hasAdministrator());
        assertEquals("JWT Permissions service error for hasAdministrator no longer required", exception.getMessage());
    }

    @Test
    public void addAdministrator_Email_Sessions_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.addAdministrator(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for addAdministrator no longer required", exception.getMessage());
    }

    @Test
    public void removeAdministrator_EMail_Sessions_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.removeAdministrator(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for removeAdministrator no longer required", exception.getMessage());
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
    public void canEdit_email_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.canEdit(TEST_USER_EMAIL));
        assertEquals("JWT Permissions service error for canEdit no longer required", exception.getMessage());
    }

    @Test
    public void canEdit_User_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.canEdit(user_Mock));
        assertEquals("JWT Permissions service error for canEdit no longer required", exception.getMessage());
    }

    @Test
    public void addEditor_EMail_Sessions_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.addEditor(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for addEditor no longer required", exception.getMessage());
    }

    @Test
    public void removeEditor_EMail_Sessions_ShouldError() throws UnsupportedOperationExceptions {
        UnsupportedOperationExceptions exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.removeEditor(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for removeEditor no longer required", exception.getMessage());
    }

    @Test
    public void canView_Session_CollectionDescription() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));
        assertTrue(jwtPermissionsService.canView(session, collectionDescriptionMock));
    }

    @Test
    public void canView_Session_Null_CollectionDescription() throws IOException {
        Session session = new Session();
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        IOException exception = assertThrows(IOException.class, () ->
                jwtPermissionsService.canView(session, collectionDescriptionMock));
        assertEquals("Empty or Null Session or CollectionDescription", exception.getMessage());
    }

    @Test
    public void canView_Session_GroupNull_CollectionDescription() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setId(FLORENCE_TOKEN);
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        Exception exception = assertThrows(IOException.class, () ->
                jwtPermissionsService.canView(session, collectionDescriptionMock));
        assertEquals("Empty or Null Session or CollectionDescription", exception.getMessage());
    }

    @Test
    public void canView_Session_CollectionDescription_Null() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);
        CollectionDescription collectionDescriptionMock = new CollectionDescription();

        Exception exception = assertThrows(IOException.class, () ->
                jwtPermissionsService.canView(session, collectionDescriptionMock));
        assertEquals("Empty or Null Session or CollectionDescription", exception.getMessage());
    }

    @Test
    public void canView_Session_CollectionDescription_TeamNull() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");

        Exception exception = assertThrows(IOException.class, () ->
                jwtPermissionsService.canView(session, collectionDescriptionMock));
        assertEquals("Empty or Null Session or CollectionDescription", exception.getMessage());
    }

    @Test
    public void canView_EMail_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.canView(TEST_USER_EMAIL, collectionDescriptionMock));
        assertEquals("JWT Permissions service error for canView no longer required", exception.getMessage());
    }

    @Test
    public void canView_User_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.canView(user_Mock, collectionDescriptionMock));
        assertEquals("JWT Permissions service error for canView no longer required", exception.getMessage());
    }

    @Test
    public void addViewerTeam_CollectionDescription_Team_Session_collectionTeam() throws Exception {

        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        Integer t = 6;

        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        Set<Integer> teamList = new HashSet<Integer>() {{
            add(123456);
            add(789012345);
        }};

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put("1234", teamList);

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);
        when(jwtPermissionStore_Mock.getAccessMapping()).thenReturn(accessMapping_Mock);

        jwtPermissionsService.addViewerTeam(collectionDescriptionMock, t, session);

        assertTrue(collectionMapping.get("1234").size() > 2);
        assertTrue(collectionMapping.get("1234").contains(t));

    }

    @Test
    public void addViewerTeam_CollectionDescription_Team_SessionNull_shouldError() throws Exception {
        Session session = new Session();

        Integer t = 6;

        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));
        Exception exception = assertThrows(UnauthorizedException.class, () ->
                jwtPermissionsService.addViewerTeam(collectionDescriptionMock, t, session));
        assertEquals("You do not have the right permission: null (null)", exception.getMessage());
    }

    @Test
    public void addViewerTeam_CollectionDescription_Team_Session_collectionTeam_nullcollectionMapping() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        Integer t = 6;

        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);
        when(jwtPermissionStore_Mock.getAccessMapping()).thenReturn(accessMapping_Mock);

        jwtPermissionsService.addViewerTeam(collectionDescriptionMock, t, session);
        assertThat(collectionMapping, IsMapContaining.hasKey("1234"));
        assertTrue(collectionMapping.get("1234").size() > 0);
        assertTrue(collectionMapping.get("1234").contains(t));
        assertFalse(collectionMapping.get("1234").contains(123456));
        
    }

    @Test
    public void listViewerTeams_collectionDescription_session() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        Set<Integer> teamList = new HashSet<Integer>() {{
            add(123456);
            add(789012345);
        }};
        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put("1234", teamList);

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);
        when(jwtPermissionStore_Mock.getAccessMapping()).thenReturn(accessMapping_Mock);

        Set<Integer> actual = jwtPermissionsService.listViewerTeams(collectionDescriptionMock, session);
        assertThat(actual, is(notNullValue()));
        assertFalse(actual.isEmpty());
        assertTrue(actual.stream().anyMatch(c -> c.equals(789012345)));
        assertTrue(actual.stream().anyMatch(c -> c.equals(123456)));
        assertFalse(actual.stream().anyMatch(c -> c.equals(666)));
    }

    @Test
    public void listViewerTeams_collectionDescription_session_nullTeam() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put("1234", null);

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);
        when(jwtPermissionStore_Mock.getAccessMapping()).thenReturn(accessMapping_Mock);

        Set<Integer> actual = jwtPermissionsService.listViewerTeams(collectionDescriptionMock, session);
        assertThat(actual, is(notNullValue()));
        assertTrue(actual.isEmpty());

    }

    @Test
    public void listViewerTeams_CollectionDescription_session_Null() throws Exception {
        Session session = null;
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));
        Exception exception = assertThrows(IOException.class, () ->
                jwtPermissionsService.listViewerTeams(collectionDescriptionMock, session));
        assertEquals("Empty or Null Session or CollectionDescription", exception.getMessage());
    }

    @Test
    public void removeViewerTeam_CollectionDescription_Team_Session_collectionTeam() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        Integer t = 6;

        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        Set<Integer> teamList = new HashSet<Integer>() {{
            add(123456);
            add(789012345);
            add(t);
        }};

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();
        collectionMapping.put("1234", teamList);

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);
        when(jwtPermissionStore_Mock.getAccessMapping()).thenReturn(accessMapping_Mock);

        jwtPermissionsService.removeViewerTeam(collectionDescriptionMock, t, session);
        assertThat(collectionMapping, is(notNullValue()));
        assertTrue(collectionMapping.get("1234").size() > 0);
        assertTrue(collectionMapping.get("1234").contains(789012345));
        assertTrue(collectionMapping.get("1234").contains(123456));
        assertFalse(collectionMapping.get("1234").contains(t));
        assertFalse(collectionMapping.get("1234").contains(666));

    }


    @Test
    public void removeViewerTeam_CollectionDescription_Team_Session_collectionTeam_nullTeam() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        session.setId(FLORENCE_TOKEN);

        Integer t = 6;

        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));

        Map<String, Set<Integer>> collectionMapping = new HashMap<>();

        when(accessMapping_Mock.getCollections()).thenReturn(collectionMapping);
        when(jwtPermissionStore_Mock.getAccessMapping()).thenReturn(accessMapping_Mock);

        jwtPermissionsService.removeViewerTeam(collectionDescriptionMock, t, session);
        assertThat(collectionMapping, is(notNullValue()));
        assertTrue(collectionMapping.get("1234").size() == 0);

    }

    @Test
    public void userPermissions_EMail_Sessions_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.userPermissions(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for userPermissions no longer required", exception.getMessage());
    }

    @Test
    public void listCollectionsAccessibleByTeam_ShouldError() throws Exception {
        Team team = new Team();
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.listCollectionsAccessibleByTeam(team));
        assertEquals("JWT Permissions service error for listCollectionsAccessibleByTeam no longer required", exception.getMessage());
    }

    @Test
    public void convertGroupsToTeams() throws Exception {
        Session mocksession = new Session();
        mocksession.setEmail("dartagnan@strangerThings.com");
        mocksession.setId(FLORENCE_TOKEN);
        mocksession.setGroups(GROUP_0);
        List<Integer> result = JWTPermissionsServiceImpl.convertGroupsToTeams(mocksession);
        assertTrue(result.stream().anyMatch(c -> c.equals(123456)));
        assertTrue(result.stream().anyMatch(c -> c.equals(789012345)));
        assertFalse(result.stream().anyMatch(c -> c.equals(0)));

    }

    @Test
    public void convertGroupsToTeams_exceptionTesting_returnEmptylist() throws Exception {
        Session mocksession = new Session();
        mocksession.setEmail("dartagnan@strangerThings.com");
        mocksession.setId(FLORENCE_TOKEN);
        mocksession.setGroups(GROUP_0d);
        List<Integer> result = JWTPermissionsServiceImpl.convertGroupsToTeams(mocksession);
        assertTrue(result.isEmpty());
    }

    @Test
    public void convertGroupsToTeams_SessionNull_ShouldError() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertGroupsToTeams no groups ";
        Session session = null;
        IOException ex = assertThrows(IOException.class, () ->
                JWTPermissionsServiceImpl.convertGroupsToTeams(session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }

    @Test
    public void convertGroupsToTeams_GroupNull_ShouldError() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertGroupsToTeams no groups ";
        Session session = new Session();
        IOException ex = assertThrows(IOException.class, () ->
                JWTPermissionsServiceImpl.convertGroupsToTeams(session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }

    @Test
    public void convertGroupsToTeams_exceptionTesting_empty_session() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertGroupsToTeams no groups ";
        Session mocksession = new Session();
        IOException ex = assertThrows(IOException.class, () ->
                JWTPermissionsServiceImpl.convertGroupsToTeams(mocksession));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }

    @Test
    public void convertGroupsToTeams_exceptionTesting_empty_sessiongroup() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertGroupsToTeams no groups ";
        Session mocksession = new Session();
        mocksession.setEmail("dartagnan@strangerThings.com");
        mocksession.setId(FLORENCE_TOKEN);
        IOException ex = assertThrows(IOException.class, () ->
                JWTPermissionsServiceImpl.convertGroupsToTeams(mocksession));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }

    @Test
    public void convertTeamsListStringToListInteger() throws Exception {
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0));
        List<Integer> result = JWTPermissionsServiceImpl.convertTeamsListStringToListInteger(collectionDescriptionMock);
        assertTrue(result.stream().anyMatch(c -> c.equals(123456)));
        assertTrue(result.stream().anyMatch(c -> c.equals(789012345)));
        assertFalse(result.stream().anyMatch(c -> c.equals(987)));

    }

    @Test
    public void convertTeamsListStringToListInteger_returnEmptylist() throws Exception {
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(Arrays.asList(GROUP_0d));
        List<Integer> result = JWTPermissionsServiceImpl.convertTeamsListStringToListInteger(collectionDescriptionMock);
        assertTrue(result.isEmpty());
    }

    @Test
    public void convertTeamsListStringToListInteger_CollectionDescriptionNull_ShouldError() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertTeamsListStringToListInteger no teams ";
        CollectionDescription collectionDescriptionMock = null;
        IOException ex = assertThrows(IOException.class, () ->
                JWTPermissionsServiceImpl.convertTeamsListStringToListInteger(collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }


    @Test
    public void convertTeamsListStringToListInteger_CollectionDescriptionTeamNull_ShouldError() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertTeamsListStringToListInteger no teams ";
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        collectionDescriptionMock.setTeams(null);
        IOException ex = assertThrows(IOException.class, () ->
                JWTPermissionsServiceImpl.convertTeamsListStringToListInteger(collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }

    @Test
    public void convertTeamsListStringToListInteger_exceptionTesting_empty_CollectionDescription() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertTeamsListStringToListInteger no teams ";
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        IOException ex = assertThrows(IOException.class, () ->
                JWTPermissionsServiceImpl.convertTeamsListStringToListInteger(collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }

    @Test
    public void convertTeamsListStringToListInteger_exceptionTesting_empty_sessiongroup() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertTeamsListStringToListInteger no teams ";
        CollectionDescription collectionDescriptionMock = new CollectionDescription();
        collectionDescriptionMock.setId("1234");
        List<String> mockList = new ArrayList<String>();
        collectionDescriptionMock.setTeams(mockList);
        IOException ex = assertThrows(IOException.class, () ->
                JWTPermissionsServiceImpl.convertTeamsListStringToListInteger(collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }

}
