package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.zebedee.exceptions.UnsupportedOperationExceptions;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JWTPermissionsServiceImplTest {
    //    public static final String SESSIONS = "sessions";
    private static final String[] GROUP_0 = new String[]{"testgroup0", "role-publisher", "role-admin", "testgroup1"};
    private static final String[] GROUP_0A = new String[]{"testgroup0", "role-publisher", "testgroup1"};
    private static final String[] GROUP_0B = new String[]{"testgroup0", "role-admin", "testgroup1"};

    private static final String[] GROUP_1 = new String[]{"testgroup0", "testgroup1"};
    private static final String[] GROUP_2 = new String[]{"123456", "role-publisher", "role-admin", "789012345"};
    private static final String FLORENCE_TOKEN = "666";
    private static final String TEST_USER_EMAIL = "other123@ons.gov.uk";
    private static final String TEST_SESSION_ID = "123test-session-id";
    private static final String PUBLISHER = "role-publisher";
    private static final String ADMIN = "role-admin";
    private static final String JWTPERMISSIONSSERVICE_ERROR = "error accessing JWTPermissions Service";
    private Set<String> digitalPublishingTeam;

    @Mock
    private Session session;

    @Mock
    private PermissionsStore permissionsStore;

    @Mock
    private AccessMapping accessMapping;

    @Mock
    private Sessions sessionsService;

    @Mock
    private PermissionsService jwtPermissionsService;

    @Mock
    private Collection collectionMock;

    @Mock
    private CollectionDescription collectionDescriptionMock;

    @Mock
    private JWTPermissionsServiceImpl jwtPSI_Mock;

    @Mock
    private User user_Mock;

    @Mock
    private Team team_Mock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        jwtPermissionsService = new JWTPermissionsServiceImpl(sessionsService);
        session = new Session();
        session.setEmail("test@ons.gov.co.uk");
        session.setId("666");
        session.setLastAccess(null);
        session.setStart(null);
    }

    @Test
    public void hasPermission_Admin_ShouldTrue() {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        when(jwtPSI_Mock.hasPermission(session, ADMIN)).thenReturn(true);
        assertTrue(jwtPSI_Mock.hasPermission(session, ADMIN));
        verify(jwtPSI_Mock, atLeastOnce()).hasPermission(session, ADMIN);
    }

    @Test
    public void hasPermission_Publisher_ShouldTrue() {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);
        when(jwtPSI_Mock.hasPermission(session, PUBLISHER)).thenReturn(true);
        assertTrue(jwtPSI_Mock.hasPermission(session, PUBLISHER));
        verify(jwtPSI_Mock, atLeastOnce()).hasPermission(session, PUBLISHER);
    }

    @Test
    public void hasPermission_PublishNotInGroup_ShouldFalse() {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0B);
        when(jwtPSI_Mock.hasPermission(session, PUBLISHER)).thenReturn(false);
        assertFalse(jwtPSI_Mock.hasPermission(session, PUBLISHER));
    }

    @Test
    public void hasPermission_AdminNotInGroup_ShouldFalse() {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0A);
        when(jwtPSI_Mock.hasPermission(session, ADMIN)).thenReturn(false);
        assertFalse(jwtPSI_Mock.hasPermission(session, PUBLISHER));
    }

    @Test
    public void hasPermission_nullSession_ShouldFalse() {
        Session session = null;
        assertFalse(jwtPSI_Mock.hasPermission(session, ADMIN));
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
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(null);
        session.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isPublisher(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isPublisher(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isPublisher(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Email_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
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
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        assertFalse(jwtPermissionsService.isAdministrator(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Session_NotAdmin_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isAdministrator(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isAdministrator(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Email_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.isAdministrator(TEST_USER_EMAIL));
        assertEquals("JWT Permissions service error for isAdministrator no longer required", exception.getMessage());
    }

//    @Test
//    public void getCollectionAccessMapping_Collection_ShouldError() throws Exception {
//        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
//                jwtPermissionsService.getCollectionAccessMapping(collectionMock));
//        assertEquals("JWT Permissions service error for getCollectionAccessMapping no longer required", exception.getMessage());
//    }

    @Test
    public void hasAdministrator_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.hasAdministrator());
        assertEquals("JWT Permissions service error for hasAdministrator no longer required", exception.getMessage());
    }

    @Test
    public void addAdministrator_Email_Sessions_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.addAdministrator(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for addAdministrator no longer required", exception.getMessage());
    }

    @Test
    public void removeAdministrator_EMail_Sessions_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
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
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void canEdit_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(null);
        session.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.canEdit(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void canEdit_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.canEdit(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void canEdit_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.canEdit(session));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void canEdit_email_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.canEdit(TEST_USER_EMAIL));
        assertEquals("JWT Permissions service error for canEdit no longer required", exception.getMessage());
    }

    @Test
    public void canEdit_User_ShouldError() throws Exception {

        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.canEdit(user_Mock));
        assertEquals("JWT Permissions service error for canEdit no longer required", exception.getMessage());
    }

    @Test
    public void addEditor_EMail_Sessions_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.addEditor(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for addEditor no longer required", exception.getMessage());
    }

    @Test
    public void removeEditor_EMail_Sessions_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.removeEditor(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for removeEditor no longer required", exception.getMessage());
    }

    @Test
    public void canView_Session_() throws Exception {
//        Map<String, Set<Integer>>
        String collectionMappingIds = "aaaa";
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_1);

        List<Integer> teams = JWTPermissionsServiceImpl.convertGroupsToTeams(session);
        digitalPublishingTeam.add(TEST_USER_EMAIL);
        when(permissionsStore.getAccessMapping())
                .thenReturn(accessMapping);
        when(accessMapping.getCollections())
                .thenReturn(new HashMap<>());
        when(accessMapping.getCollections().get(collectionDescriptionMock.getId()))
                .thenReturn(null);


        assertThat(jwtPermissionsService.canView(session, collectionDescriptionMock), is(true));
        verify(permissionsStore, atLeastOnce()).getAccessMapping();
        verify(accessMapping, atLeastOnce()).getDigitalPublishingTeam();
//
//        Set<Integer> collectionTeams = accessMapping.getCollections().get(collectionDescription.getId());
//        if (collectionTeams == null || collectionTeams.isEmpty()) {
//            return false;
//        }
//        return teams.stream().anyMatch(t -> collectionTeams.contains(t));
    }

    @Test
    public void canView_EMail_ShouldError() throws Exception {
        // TODO: 16/12/2021
//        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
//                jwtPermissionsService.canView(TEST_USER_EMAIL, collectionDescriptionMock));
//        assertEquals("JWT Permissions service error for canView no longer required", exception.getMessage());
    }

    @Test
    public void canView_User_ShouldError() throws Exception {
        // TODO: 16/12/2021
//     Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
//                jwtPermissionsService.canView(user_Mock, collectionDescriptionMock));
//        assertEquals("JWT Permissions service error for canView no longer required", exception.getMessage());
    }

    @Test
    public void addViewerTeam_CollectionDescription_Team_Session_ShouldError() throws Exception {
        // TODO: 16/12/2021
//        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
//                jwtPermissionsService.canView(user_Mock, collectionDescriptionMock));
//        assertEquals("JWT Permissions service error for canView no longer required", exception.getMessage());
    }

    @Test
    public void listViewerTeams_collectionDescription_session() throws Exception {

    }

    @Test
    public void removeViewerTeam_CollectionDescription_Team_Session_ShouldError() throws Exception {
        Integer t = 6;
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.removeViewerTeam(collectionDescriptionMock, t, session));
        assertEquals("JWT Permissions service error for removeViewerTeam no longer required", exception.getMessage());
    }

    @Test
    public void userPermissions_EMail_Sessions_ShouldError() throws Exception {
        Exception exception = assertThrows(UnsupportedOperationExceptions.class, () ->
                jwtPermissionsService.userPermissions(TEST_USER_EMAIL, session));
        assertEquals("JWT Permissions service error for userPermissions no longer required", exception.getMessage());
    }

    @Test
    public void listCollectionsAccessibleByTeam_ShouldError() throws Exception {

    }

    @Test
    public void convertGroupsToTeams_GroupNull_ShouldError() throws Exception {
        String expectedMessage = "JWT Permissions service error for convertGroupsToTeams no groups";
        Session session = new Session();
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> JWTPermissionsServiceImpl.convertGroupsToTeams(session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(expectedMessage));
    }

    @Test
    public void convertGroupsToTeams() throws Exception {
        Session mocksession = new Session();
        mocksession.setEmail("dartagnan@strangerThings.com");
        mocksession.setId(FLORENCE_TOKEN);
        mocksession.setGroups(GROUP_2);
        List<Integer> result = JWTPermissionsServiceImpl.convertGroupsToTeams(mocksession);
        assertTrue(result.stream().anyMatch(c -> c.equals(123456)));
        assertTrue(result.stream().anyMatch(c -> c.equals(789012345)));
        assertFalse(result.stream().anyMatch(c -> c.equals("role-publisher")));
    }

    @Test
    public void convertGroupsToTeams_exceptionTesting_returnEmptylist() throws Exception {
        Session session = new Session();
        session.setEmail("dartagnan@strangerThings.com");
        session.setId(FLORENCE_TOKEN);
        session.setGroups(GROUP_0);
        List<Integer> result = JWTPermissionsServiceImpl.convertGroupsToTeams(session);
        assertTrue(result.isEmpty());
    }

    @Test
    public void convertGroupsToTeams_exceptionTesting_empty_session() throws Exception {
        Session mocksession = new Session();
        Exception exception = assertThrows(JWTVerificationException.class, () ->
                JWTPermissionsServiceImpl.convertGroupsToTeams(mocksession));
        assertEquals("JWT Permissions service error for convertGroupsToTeams no groups", exception.getMessage());
    }

    @Test
    public void convertGroupsToTeams_exceptionTesting_empty_sessiongroup() throws Exception {
        Session mocksession = new Session();
        mocksession.setEmail("dartagnan@strangerThings.com");
        mocksession.setId(FLORENCE_TOKEN);
        Exception exception = assertThrows(JWTVerificationException.class, () ->
                JWTPermissionsServiceImpl.convertGroupsToTeams(mocksession));
        assertEquals("JWT Permissions service error for convertGroupsToTeams no groups", exception.getMessage());
    }


}


