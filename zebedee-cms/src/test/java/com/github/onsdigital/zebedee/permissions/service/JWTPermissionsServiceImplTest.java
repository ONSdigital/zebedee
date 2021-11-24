package com.github.onsdigital.zebedee.permissions.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.isEmptyOrNullString;


import java.io.IOException;
import java.util.Set;

import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;                                                      

public class JWTPermissionsServiceImplTest {

    private static final String JWTPERMISSIONSSERVICE_ERROR = "error accessing JWTPermissions Service";
   
    @Mock
    private PermissionsService jwtPermissionsService;

    @Mock
    private Session session;

    @Mock
    private Collection collectionMock;
   
    @Mock
    private CollectionDescription collectionDescriptionMock;
   
    @Mock
    private Team team_mock;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        session = new Session();
    }

    @Test
    public void isPublisher_Session_ShouldError() throws Exception {
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .isPublisher(session);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.isPublisher(session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).isPublisher(session);

    }

    @Test
    public void isPublisher_Email_ShouldError() throws Exception {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .isPublisher(email);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.isPublisher(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).isPublisher(email);
    }

    @Test
    public void isAdministrator_Session_ShouldError() throws Exception {
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .isAdministrator(session);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.isAdministrator(session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).isAdministrator(session);
    }

    @Test
    public void isAdministrator_Email_ShouldError() throws Exception {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .isAdministrator(email);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.isAdministrator(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).isAdministrator(email);
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

    @Test
    public void hasAdministrator_ShouldError() throws Exception {
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .hasAdministrator();
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.hasAdministrator());
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).hasAdministrator();
    }

    @Test
    public void addAdministrator_Email_Sessions_ShouldError() throws Exception {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .addAdministrator(email, session);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.addAdministrator(email,session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).addAdministrator(email,session);
    }

    @Test
    public void removeAdministrator_EMail_Sessions_ShouldError() throws IOException, UnauthorizedException {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .removeAdministrator(email, session);

        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.removeAdministrator(email, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).removeAdministrator(email, session);
    }

    @Test
    public void canEdit_Session_ShouldError() throws IOException {
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .canEdit(session);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canEdit(session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).canEdit(session);
    }

    @Test
    public void canEdit_EMail_ShouldError() throws IOException {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .canEdit(email);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canEdit(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).canEdit(email);
    }

    @Test
    public void canEdit_User_ShouldError() throws IOException {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .canEdit(email);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canEdit(email));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).canEdit(email);
    }

    @Test
    public void addEditor_EMail_Sessions_ShouldError() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .addEditor(email, session);

        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.addEditor(email,session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).addEditor(email,session);
    }

    @Test
    public void removeEditor_EMail_Sessions_ShouldError() throws IOException, UnauthorizedException {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .removeEditor(email,session);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.removeEditor(email,session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).removeEditor(email,session);
    }

  @Test
    public void canView_Session_ShouldError() throws IOException {
        
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .canView(session, collectionDescriptionMock);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canView(session, collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).canView(session, collectionDescriptionMock);
    }

    @Test
    public void canView_EMail_ShouldError() throws IOException {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .canView(email, collectionDescriptionMock);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canView(email, collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).canView(email, collectionDescriptionMock);
    }

    @Test
    public void canView_User_ShouldError() throws IOException {
        String email = null;
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .canView(email, collectionDescriptionMock);

        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.canView(email, collectionDescriptionMock));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).canView(email, collectionDescriptionMock);
    }
    
    @Test
    public void addViewerTeam_CollectionDescription_Team_Session_ShouldError() throws IOException, ZebedeeException {
            
        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .addViewerTeam(collectionDescriptionMock,team_mock, session);
        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.addViewerTeam(collectionDescriptionMock,team_mock, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).addViewerTeam(collectionDescriptionMock,team_mock, session);
    }
    
    @Test
    public void listViewerTeams_collectionDescription_session() throws IOException, UnauthorizedException {
            doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPermissionsService)
                .listViewerTeams(collectionDescriptionMock,session);
            IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.listViewerTeams(collectionDescriptionMock,session));
            MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
            verify(jwtPermissionsService, times(1)).listViewerTeams(collectionDescriptionMock,session);
    }

    @Test
    public void removeViewerTeam_CollectionDescription_Team_Session_ShouldError() throws IOException, ZebedeeException {
            doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPermissionsService)
                .removeViewerTeam(collectionDescriptionMock,team_mock, session);
    
            IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.removeViewerTeam(collectionDescriptionMock,team_mock, session));
            MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
            verify(jwtPermissionsService, times(1)).removeViewerTeam(collectionDescriptionMock,team_mock, session);
    }

    @Test
    public void userPermissions_EMail_Sessions_ShouldError() throws IOException, NotFoundException, UnauthorizedException {
        String email = null;

        doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
            .when(jwtPermissionsService)
            .userPermissions(email, session);

        IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.userPermissions(email, session));
        MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
        verify(jwtPermissionsService, times(1)).userPermissions(email, session);
    }
    
    @Test
    public void listCollectionsAccessibleByTeam_ShouldError() throws IOException {
            doThrow(new IOException(JWTPERMISSIONSSERVICE_ERROR))
                .when(jwtPermissionsService)
                .listCollectionsAccessibleByTeam(team_mock);
    
            IOException ex = assertThrows(IOException.class, () -> jwtPermissionsService.listCollectionsAccessibleByTeam(team_mock));
            MatcherAssert.assertThat(ex.getMessage(), equalTo(JWTPERMISSIONSSERVICE_ERROR));
            verify(jwtPermissionsService, times(1)).listCollectionsAccessibleByTeam(team_mock);
    }
    
}