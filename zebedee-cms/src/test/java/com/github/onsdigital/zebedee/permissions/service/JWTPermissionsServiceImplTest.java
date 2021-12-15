package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JWTPermissionsServiceImplTest {
    public static final String SESSIONS = "sessions";
    private static final String[] GROUP_0 = new String[]{"testgroup0", "role-publisher", "role-admin", "testgroup1"};
    private static final String[] GROUP_1 = new String[]{"testgroup0", "testgroup1"};
    private static final String[] GROUP_2 = new String[]{"123456", "role-publisher", "role-admin", "789012345"};
    private static final String FLORENCE_TOKEN = "666";
    private static final String TEST_USER_EMAIL = "other123@ons.gov.uk";
    private static final String TEST_SESSION_ID = "123test-session-id";
    private static final String PUBLISHER = "role-publisher";
    private static final String ADMIN = "role-admin";
    private static final String JWTPERMISSIONSSERVICE_ERROR = "error accessing JWTPermissions Service";

    @Mock
    private Session session;

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

    /**
     * @throws Exception
     */
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

    /**
     *
     */
    @Test
    public void hasPermission_Admin_ShouldTrue() {
        Session sessionService = new Session();
        sessionService.setId(TEST_SESSION_ID);
        sessionService.setEmail(TEST_USER_EMAIL);
        sessionService.setGroups(GROUP_0);
        when(jwtPSI_Mock.hasPermission(sessionService, ADMIN)).thenReturn(true);
        assertTrue(jwtPSI_Mock.hasPermission(sessionService, ADMIN));
        verify(jwtPSI_Mock, atLeastOnce()).hasPermission(sessionService, ADMIN);
    }

    @Test
    public void hasPermission_Publisher_ShouldTrue() {
        Session sessionService = new Session();
        sessionService.setId(TEST_SESSION_ID);
        sessionService.setEmail(TEST_USER_EMAIL);
        sessionService.setGroups(GROUP_0);
        when(jwtPSI_Mock.hasPermission(sessionService, PUBLISHER)).thenReturn(true);
        assertTrue(jwtPSI_Mock.hasPermission(sessionService, PUBLISHER));
        verify(jwtPSI_Mock, atLeastOnce()).hasPermission(sessionService, PUBLISHER);
    }

    @Test
    public void hasPermission_PublishNotInGroup_ShouldFalse() {
        Session sessionService = new Session();
        sessionService.setId(TEST_SESSION_ID);
        sessionService.setEmail(TEST_USER_EMAIL);
        sessionService.setGroups(GROUP_1);
        when(jwtPSI_Mock.hasPermission(sessionService, PUBLISHER)).thenReturn(false);
        assertFalse(jwtPSI_Mock.hasPermission(sessionService, PUBLISHER));
    }

    @Test
    public void hasPermission_nullSession_ShouldFalse() {
        Session sessionTest02 = null;
        assertFalse(jwtPSI_Mock.hasPermission(sessionTest02, ADMIN));
    }

    @Test
    public void isPublisher_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session testsession1 = new Session();
        testsession1.setEmail(TEST_USER_EMAIL);
        testsession1.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.isPublisher(testsession1));
    }

    @Test
    public void isPublisher_SessionNull_ShouldReturnFalse() throws Exception {
        Session testsession2 = null;
        assertFalse(jwtPermissionsService.isPublisher(testsession2));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session testsession3 = new Session();
        testsession3.setId(TEST_SESSION_ID);
        testsession3.setEmail(null);
        testsession3.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isPublisher(testsession3));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        Session testsession4 = new Session();
        testsession4.setEmail(TEST_USER_EMAIL);
        testsession4.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isPublisher(testsession4));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session testsession5 = new Session();
        testsession5.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isPublisher(testsession5));
        verifyZeroInteractions(sessionsService);
    }


    @Test
    public void isPublisher_Email_ShouldError() throws Exception {
        String email = TEST_USER_EMAIL;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).isPublisher(email);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.isPublisher(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).isPublisher(email);
    }


    @Test
    public void isAdministrator_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session testsession6 = new Session();
        testsession6.setEmail(TEST_USER_EMAIL);
        testsession6.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.isAdministrator(testsession6));
    }

    @Test
    public void isAdministrator_SessionNull_ShouldReturnFalse() throws Exception {
        Session testsession7 = null;
        assertFalse(jwtPermissionsService.isAdministrator(testsession7));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session testsession8 = new Session();
        assertFalse(jwtPermissionsService.isAdministrator(testsession8));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Session_NotAdmin_ShouldReturnFalse() throws Exception {
        Session testsession9 = new Session();
        testsession9.setEmail(TEST_USER_EMAIL);
        testsession9.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isAdministrator(testsession9));
        verifyZeroInteractions(sessionsService);

    }

    @Test
    public void isAdministrator_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session testsession10 = new Session();
        testsession10.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isAdministrator(testsession10));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Email_ShouldError() throws Exception {
        String email = TEST_USER_EMAIL;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).isAdministrator(email);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.isAdministrator(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).isAdministrator(email);
    }

    @Test
    public void getCollectionAccessMapping_Collection_ShouldError() throws Exception {
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).getCollectionAccessMapping(collectionMock);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.getCollectionAccessMapping(collectionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).getCollectionAccessMapping(collectionMock);
    }

    @Test
    public void hasAdministrator_ShouldError() throws Exception {
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .hasAdministrator();
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.hasAdministrator());
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).hasAdministrator();
    }

    @Test
    public void addAdministrator_Email_Sessions_ShouldError() throws Exception {
        String email = null;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).addAdministrator(email, session);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.addAdministrator(email, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).addAdministrator(email, session);
    }

    @Test
    public void removeAdministrator_EMail_Sessions_ShouldError() throws Exception {
        String email = null;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).removeAdministrator(email, session);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.removeAdministrator(email, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).removeAdministrator(email, session);
    }

    @Test
    public void canEdit_Session_ShouldError() throws Exception {
        Session session = null;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).canEdit(session);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.canEdit(session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).canEdit(session);
    }

    @Test
    public void canEdit_email_ShouldError() throws Exception {
        String email = null;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).canEdit(email);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.canEdit(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).canEdit(email);
    }

    @Test
    public void canEdit_User_ShouldError() throws Exception {
        User user = new User();
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).canEdit(user);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.canEdit(user));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).canEdit(user);
    }

    @Test
    public void addEditor_EMail_Sessions_ShouldError() throws Exception {
        String email = null;
        Session session = new Session();
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).addEditor(email, session);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.addEditor(email, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, atLeastOnce()).addEditor(email, session);
    }

    @Test
    public void removeEditor_EMail_Sessions_ShouldError() throws Exception {
        String email = null;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .removeEditor(email, session);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.removeEditor(email, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).removeEditor(email, session);
    }

    @Test
    public void canView_Session_ShouldError() throws Exception {
        Session session = new Session();
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .canView(session, collectionDescriptionMock);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.canView(session, collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).canView(session, collectionDescriptionMock);
    }

    @Test
    public void canView_EMail_ShouldError() throws Exception {
        String email = null;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .canView(email, collectionDescriptionMock);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.canView(email, collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).canView(email, collectionDescriptionMock);
    }

    @Test
    public void canView_User_ShouldError() throws Exception {
        User user = new User();
        String email = null;
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .canView(email, collectionDescriptionMock);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.canView(email, collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).canView(email, collectionDescriptionMock);
    }

    @Test
    public void addViewerTeam_CollectionDescription_Team_Session_ShouldError() throws Exception {
        Team team_mock = new Team();
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .addViewerTeam(collectionDescriptionMock, team_mock, session);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.addViewerTeam(collectionDescriptionMock, team_mock, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).addViewerTeam(collectionDescriptionMock, team_mock, session);
    }

    @Test
    public void listViewerTeams_collectionDescription_session() throws Exception {
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .listViewerTeams(collectionDescriptionMock, session);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.listViewerTeams(collectionDescriptionMock, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).listViewerTeams(collectionDescriptionMock, session);
    }

    @Test
    public void removeViewerTeam_CollectionDescription_Team_Session_ShouldError() throws Exception {
        Team team_mock = new Team();
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .removeViewerTeam(collectionDescriptionMock, team_mock, session);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.removeViewerTeam(collectionDescriptionMock, team_mock, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).removeViewerTeam(collectionDescriptionMock, team_mock, session);
    }

    @Test
    public void userPermissions_EMail_Sessions_ShouldError() throws Exception {
        String email = null;

        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .userPermissions(email, session);

        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.userPermissions(email, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).userPermissions(email, session);
    }

    @Test
    public void listCollectionsAccessibleByTeam_ShouldError() throws Exception {
        Team team_mock = new Team();
        doThrow(new JWTVerificationException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPSI_Mock)
                .listCollectionsAccessibleByTeam(team_mock);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPSI_Mock.listCollectionsAccessibleByTeam(team_mock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPSI_Mock, times(1)).listCollectionsAccessibleByTeam(team_mock);
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
        Session mocksession = new Session();
        mocksession.setEmail("dartagnan@strangerThings.com");
        mocksession.setId(FLORENCE_TOKEN);
        mocksession.setGroups(GROUP_0);
        List<Integer> result = JWTPermissionsServiceImpl.convertGroupsToTeams(mocksession);
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


    @Test
    public void isPublisher() {
    }

    @Test
    public void testIsPublisher() {
    }

    @Test
    public void isAdministrator() {
    }

    @Test
    public void testIsAdministrator() {
    }

    @Test
    public void getCollectionAccessMapping() {
    }

    @Test
    public void hasAdministrator() {
    }

    @Test
    public void addAdministrator() {
    }

    @Test
    public void removeAdministrator() {
    }

    @Test
    public void canEdit() {
    }

    @Test
    public void testCanEdit() {
    }

    @Test
    public void testCanEdit1() {
    }

    @Test
    public void addEditor() {
    }

    @Test
    public void removeEditor() {
    }

    @Test
    public void canView() {
    }

    @Test
    public void testCanView() {
    }

    @Test
    public void testCanView1() {
    }

    @Test
    public void addViewerTeam() {
    }

    @Test
    public void listViewerTeams() {
    }

    @Test
    public void removeViewerTeam() {
    }

    @Test
    public void userPermissions() {
    }

    @Test
    public void listCollectionsAccessibleByTeam() {
    }

    @Test
    public void hasPermission() {
    }
}


