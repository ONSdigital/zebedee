package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.*;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.session.store.JWTStore;
import com.github.onsdigital.zebedee.teams.model.Team;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JWTPermissionsServiceImplTest {

    private static final String[] GROUP_0 = new String[]{"testgroup0", "publisher", "admin", "testgroup1"};
    private static final String[] GROUP_1 = new String[]{"testgroup0", "testgroup1"};
    private static final String TEST_USER_EMAIL = "other123@ons.gov.uk";
    private static final String TEST_SESSION_ID = "123test-session-id";
    private static final String PUBLISHER = "publisher";
    private static final String ADMIN = "admin";

    private static final String JWTPERMISSIONSSERVICE_ERROR = "error accessing JWTPermissions Service";

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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        jwtPermissionsService = new JWTPermissionsServiceImpl(sessionsService);

        session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);

    }

    @Test
    public void isPublisher_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session s = new Session();
        s.setEmail(TEST_USER_EMAIL);
        s.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.isPublisher(s));
    }

    @Test
    public void isPublisher_SessionNull_ShouldReturnFalse() throws Exception {
        Session s = null;
        assertFalse(jwtPermissionsService.isPublisher(s));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session s = new Session();
        assertFalse(jwtPermissionsService.isPublisher(s));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_NotPublisher_ShouldReturnFalse() throws Exception {
        Session s = new Session();
        s.setEmail(TEST_USER_EMAIL);
        s.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isPublisher(s));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session s = new Session();
        s.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isPublisher(s));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isPublisher_Email_ShouldError() throws Exception {
        String email = null;
       doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
               .when(jwtPermissionsService)
               .isPublisher(email);
        JWTVerificationException ex = assertThrows(JWTVerificationException.class, () -> jwtPermissionsService.isPublisher(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Session_Publisher_ShouldReturnTrue() throws Exception {
        Session s = new Session();
        s.setEmail(TEST_USER_EMAIL);
        s.setGroups(GROUP_0);
        assertTrue(jwtPermissionsService.isAdministrator(s));
    }

    @Test
    public void isAdministrator_SessionNull_ShouldReturnFalse() throws Exception {
        Session s = null;
        assertFalse(jwtPermissionsService.isAdministrator(s));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Session_EmailNull_ShouldReturnFalse() throws Exception {
        Session s = new Session();
        assertFalse(jwtPermissionsService.isAdministrator(s));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Session_NotAdmin_ShouldReturnFalse() throws Exception {
        Session s = new Session();
        s.setEmail(TEST_USER_EMAIL);
        s.setGroups(GROUP_1);
        assertFalse(jwtPermissionsService.isAdministrator(s));
        verifyZeroInteractions(sessionsService);

    }

    @Test
    public void isAdministrator_Session_NullGroup_ShouldReturnFalse() throws Exception {
        Session s = new Session();
        s.setEmail(TEST_USER_EMAIL);
        assertFalse(jwtPermissionsService.isAdministrator(s));
        verifyZeroInteractions(sessionsService);
    }

    @Test
    public void isAdministrator_Email_ShouldReturnTrue() throws Exception {
        String email = TEST_USER_EMAIL;
        Session session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);
        session.setGroups(GROUP_0);

        when(sessionsService.get(email))
                .thenReturn(session);

        when(((JWTPermissionsServiceImpl) jwtPermissionsService).hasPermission(session, session.getEmail()))
                .thenReturn(true);

        assertTrue(jwtPermissionsService.isAdministrator(email));
        verifyZeroInteractions(sessionsService);
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
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPermissionsService)
                .getCollectionAccessMapping(collectionMock);
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