package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.session.store.JWTStore;
import com.github.onsdigital.zebedee.session.store.SessionsStore;
import com.github.onsdigital.zebedee.session.store.SessionsStoreImpl;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JWTPermissionsServiceImplTest {
    public static final String SESSIONS = "sessions";
    private static final String[] GROUP_0 = new String[]{"testgroup0", "publisher", "admin", "testgroup1"};
    private static final String[] GROUP_1 = new String[]{"testgroup0", "testgroup1"};
    private static final String TEST_USER_EMAIL = "other123@ons.gov.uk";
    private static final String TEST_SESSION_ID = "123test-session-id";
    private static final String PUBLISHER = "publisher";
    private static final String ADMIN = "admin";
    private static final String JWTPERMISSIONSSERVICE_ERROR = "error accessing JWTPermissions Service";
    @Rule
    public TemporaryFolder root = new TemporaryFolder();
    @Mock
    protected HttpServletRequest mockRequest;
    @Mock
    private PermissionsService jwtPermissionsService;
    @Mock
    private Session session;
    @Mock
    private JWTStore jwtStore;
    @Mock
    private Sessions sessionsService;
    @Mock
    private Collection collectionMock;
    @Mock
    private CollectionDescription collectionDescriptionMock;
    @Mock
    private JWTPermissionsServiceImpl jwtPSI_Mock;
    @Mock
    private Team team_mock;
    private SessionsStore sessionsStore;
    private File sessionsDir;
    private Date lastAccessed;
    private Date startDate;
    private Path sessionFile;

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        jwtPermissionsService = new JWTPermissionsServiceImpl(sessionsService);
        root.create();
        sessionsDir = root.newFolder("sessions");
        lastAccessed = new Date();
        startDate = new Date();

        session = new Session();
        session.setEmail("test@ons.gov.co.uk");
        session.setId("666");
        session.setLastAccess(null);
        session.setStart(null);

        String fileName = UUID.randomUUID().toString();
        sessionFile = sessionsDir.toPath().resolve(fileName + ".json");
        sessionsStore = new SessionsStoreImpl(sessionsDir.toPath());
    }

    @Test
    public void getSessionfromEmail_ShouldReturnSession() throws Exception {
        Session sessionTest01 = new Session();
        sessionTest01.setId(TEST_SESSION_ID);
        sessionTest01.setEmail(TEST_USER_EMAIL);
        sessionTest01.setGroups(GROUP_1);
        when(jwtPSI_Mock.getSessionfromEmail(TEST_USER_EMAIL)).thenReturn(sessionTest01);
        Session expectedSession = jwtPSI_Mock.getSessionfromEmail(TEST_USER_EMAIL);
        assertEquals(expectedSession.getEmail(), TEST_USER_EMAIL);
        assertArrayEquals(GROUP_1, expectedSession.getGroups());
        assertEquals(expectedSession.getId(), TEST_SESSION_ID);
    }

    @Test
    public void getSessionfromEmail_ShouldReturnNull() {
        when(jwtPSI_Mock.getSessionfromEmail(TEST_USER_EMAIL)).thenReturn(null);
        assertNull(jwtPSI_Mock.getSessionfromEmail(TEST_USER_EMAIL));
    }

    @Test
    public void hasPermission_Admin_ShouldTrue() {
        Session sessionTest02 = new Session();
        sessionTest02.setId(TEST_SESSION_ID);
        sessionTest02.setEmail(TEST_USER_EMAIL);
        sessionTest02.setGroups(GROUP_0);
        assertTrue(jwtPSI_Mock.hasPermission(sessionTest02, ADMIN));
    }

    @Test
    public void hasPermission_ShouldFalse() {
        Session sessionTest02 = new Session();
        sessionTest02.setId(TEST_SESSION_ID);
        sessionTest02.setEmail(TEST_USER_EMAIL);
        sessionTest02.setGroups(GROUP_1);
        assertFalse(jwtPSI_Mock.hasPermission(sessionTest02, ADMIN));
    }

    @Test
    public void hasPermission_nullSession_ShouldFalse() {
        Session sessionTest02 = null;
        assertFalse(jwtPSI_Mock.hasPermission(sessionTest02, ADMIN));
    }

    /**
     * @throws Exception
     */
    @Test

    public void isPublisher_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session testsession1 = new Session();
        testsession1.setEmail(TEST_USER_EMAIL);
        testsession1.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.isPublisher(testsession1));
    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_SessionNull_ShouldReturnFalse() throws Exception {
        Session testsession2 = null;
        assertFalse(jwtPermissionsService.isPublisher(testsession2));
        verifyZeroInteractions(sessionsService);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session testsession3 = new Session();
        testsession3.setId(TEST_SESSION_ID);
        testsession3.setEmail(null);
        testsession3.setGroups(GROUP_1);

        assertFalse(jwtPermissionsService.isPublisher(testsession3));
        verifyZeroInteractions(sessionsService);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        Session testsession4 = new Session();
        testsession4.setEmail(TEST_USER_EMAIL);
        testsession4.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isPublisher(testsession4));
        verifyZeroInteractions(sessionsService);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session testsession5 = new Session();
        testsession5.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isPublisher(testsession5));
        verifyZeroInteractions(sessionsService);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isPublisher_Email_ShouldError() throws Exception {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).isPublisher(email);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPermissionsService.isPublisher(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verifyZeroInteractions(sessionsService);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session testsession6 = new Session();
        testsession6.setEmail(TEST_USER_EMAIL);
        testsession6.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.isAdministrator(testsession6));
    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_SessionNull_ShouldReturnFalse() throws Exception {
        Session testsession7 = null;
        assertFalse(jwtPermissionsService.isAdministrator(testsession7));
        verifyZeroInteractions(sessionsService);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session testsession8 = new Session();
        assertFalse(jwtPermissionsService.isAdministrator(testsession8));
        verifyZeroInteractions(sessionsService);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Session_NotAdmin_ShouldReturnFalse() throws Exception {
        Session testsession9 = new Session();
        testsession9.setEmail(TEST_USER_EMAIL);
        testsession9.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isAdministrator(testsession9));
        verifyZeroInteractions(sessionsService);

    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session testsession10 = new Session();
        testsession10.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isAdministrator(testsession10));
        verifyZeroInteractions(sessionsService);
    }

    /**
     * @throws Exception
     */
    @Test
    public void isAdministrator_Email_ShouldReturnTrue() throws Exception {
        Session testsession11 = new Session();
        testsession11.setId(TEST_SESSION_ID);
        testsession11.setEmail(TEST_USER_EMAIL);
        testsession11.setGroups(GROUP_1);
        when(sessionsService.find(TEST_USER_EMAIL))
                .thenReturn(testsession11);
        when(jwtPSI_Mock.hasPermission(null, ADMIN))
                .thenReturn(true);
        assertTrue(jwtPermissionsService.isAdministrator(TEST_USER_EMAIL));
        verifyZeroInteractions(sessionsService);
        verify(jwtPermissionsService, times(1)).getCollectionAccessMapping(collectionMock);
    }

    @Test
    public void isAdministrator_NullEmail_ShouldReturnFalse() throws Exception {
        String email = null;
        assertFalse(jwtPermissionsService.isAdministrator(email));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Email_ShouldReturnFalse() throws Exception {
        String email = TEST_USER_EMAIL;
        assertFalse(jwtPermissionsService.isAdministrator(email));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void getCollectionAccessMapping_Collection_ShouldError() throws Exception {
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR)).when(jwtPSI_Mock).getCollectionAccessMapping(collectionMock);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.getCollectionAccessMapping(collectionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).getCollectionAccessMapping(collectionMock);
    }

    // @Test
    // public void hasAdministrator_ShouldError() throws Exception {
    //     doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
    //         .when(jwtPermissionsService)
    //         .hasAdministrator();
    //     IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.hasAdministrator());
    //     MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
    //     verify(jwtPermissionsService, times(1)).hasAdministrator();
    // }

//     @Test
//     public void addAdministrator_Email_Sessions_ShouldError() throws Exception {
//         String email = null;
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .addAdministrator(email, session);
//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.addAdministrator(email,session));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).addAdministrator(email,session);
//     }

//     @Test
//     public void removeAdministrator_EMail_Sessions_ShouldError() throws IOException, UnauthorizedException {
//         String email = null;
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .removeAdministrator(email, session);

//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.removeAdministrator(email, session));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).removeAdministrator(email, session);
//     }

//     @Test
//     public void canEdit_Session_ShouldError() throws IOException {
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .canEdit(session);
//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canEdit(session));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).canEdit(session);
//     }

//     @Test
//     public void canEdit_EMail_ShouldError() throws IOException {
//         String email = null;
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .canEdit(email);
//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canEdit(email));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).canEdit(email);
//     }

//     @Test
//     public void canEdit_User_ShouldError() throws IOException {
//         String email = null;
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .canEdit(email);
//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canEdit(email));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).canEdit(email);
//     }

//     @Test
//     public void addEditor_EMail_Sessions_ShouldError() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
//         String email = null;
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .addEditor(email, session);

//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.addEditor(email,session));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).addEditor(email,session);
//     }

//     @Test
//     public void removeEditor_EMail_Sessions_ShouldError() throws IOException, UnauthorizedException {
//         String email = null;
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .removeEditor(email,session);
//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.removeEditor(email,session));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).removeEditor(email,session);
//     }

//   @Test
//     public void canView_Session_ShouldError() throws IOException {

//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .canView(session, collectionDescriptionMock);
//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canView(session, collectionDescriptionMock));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).canView(session, collectionDescriptionMock);
//     }

//     @Test
//     public void canView_EMail_ShouldError() throws IOException {
//         String email = null;
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .canView(email, collectionDescriptionMock);
//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canView(email, collectionDescriptionMock));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).canView(email, collectionDescriptionMock);
//     }

//     @Test
//     public void canView_User_ShouldError() throws IOException {
//         String email = null;
//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .canView(email, collectionDescriptionMock);

//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canView(email, collectionDescriptionMock));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).canView(email, collectionDescriptionMock);
//     }

//     @Test
//     public void addViewerTeam_CollectionDescription_Team_Session_ShouldError() throws IOException, ZebedeeException {

//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .addViewerTeam(collectionDescriptionMock,team_mock, session);
//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.addViewerTeam(collectionDescriptionMock,team_mock, session));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).addViewerTeam(collectionDescriptionMock,team_mock, session);
//     }

//     @Test
//     public void listViewerTeams_collectionDescription_session() throws IOException, UnauthorizedException {
//             doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//                 .when(jwtPermissionsService)
//                 .listViewerTeams(collectionDescriptionMock,session);
//             IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.listViewerTeams(collectionDescriptionMock,session));
//             MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//             verify(jwtPermissionsService, times(1)).listViewerTeams(collectionDescriptionMock,session);
//     }

//     @Test
//     public void removeViewerTeam_CollectionDescription_Team_Session_ShouldError() throws IOException, ZebedeeException {
//             doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//                 .when(jwtPermissionsService)
//                 .removeViewerTeam(collectionDescriptionMock,team_mock, session);

//             IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.removeViewerTeam(collectionDescriptionMock,team_mock, session));
//             MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//             verify(jwtPermissionsService, times(1)).removeViewerTeam(collectionDescriptionMock,team_mock, session);
//     }

//     @Test
//     public void userPermissions_EMail_Sessions_ShouldError() throws IOException, NotFoundException, UnauthorizedException {
//         String email = null;

//         doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//             .when(jwtPermissionsService)
//             .userPermissions(email, session);

//         IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.userPermissions(email, session));
//         MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//         verify(jwtPermissionsService, times(1)).userPermissions(email, session);
//     }

//     @Test
//     public void listCollectionsAccessibleByTeam_ShouldError() throws IOException {
//             doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
//                 .when(jwtPermissionsService)
//                 .listCollectionsAccessibleByTeam(team_mock);

//             IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.listCollectionsAccessibleByTeam(team_mock));
//             MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
//             verify(jwtPermissionsService, times(1)).listCollectionsAccessibleByTeam(team_mock);
//     }

}