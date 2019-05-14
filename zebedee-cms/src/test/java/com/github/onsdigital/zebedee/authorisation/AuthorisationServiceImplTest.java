package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static com.github.onsdigital.zebedee.authorisation.DatasetPermissionType.CREATE;
import static com.github.onsdigital.zebedee.authorisation.DatasetPermissionType.DELETE;
import static com.github.onsdigital.zebedee.authorisation.DatasetPermissionType.READ;
import static com.github.onsdigital.zebedee.authorisation.DatasetPermissionType.UPDATE;
import static junit.framework.TestCase.assertTrue;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AuthorisationServiceImplTest {

    private static final String SESSION_ID = "666";

    @Mock
    private SessionsService sessionsService;

    @Mock
    private UsersService usersService;

    @Mock
    private Collections collections;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private ServiceStore serviceStore;

    @Mock
    private Collection collection;

    @Mock
    private ServiceAccount serviceAccount;

    private ServiceSupplier<SessionsService> sessionServiceSupplier = () -> sessionsService;
    private ServiceSupplier<UsersService> userServiceSupplier = () -> usersService;
    private ServiceSupplier<Collections> collectionsSupplier = () -> collections;
    private ServiceSupplier<PermissionsService> permissionsServiceSupplier = () -> permissionsService;
    private ServiceSupplier<ServiceStore> serviceStoreSupplier = () -> serviceStore;

    private AuthorisationService service;
    private UserIdentityException notAuthenticatedEx;
    private UserIdentityException internalServerErrorEx;
    private UserIdentityException notFoundEx;
    private Session session;
    private CollectionDescription desc;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        notAuthenticatedEx = new UserIdentityException("user not authenticated", SC_UNAUTHORIZED);
        internalServerErrorEx = new UserIdentityException("internal server error", SC_INTERNAL_SERVER_ERROR);
        notFoundEx = new UserIdentityException("user does not exist", SC_NOT_FOUND);

        service = new AuthorisationServiceImpl();

        session = new Session();
        session.setEmail("rickSanchez@CitadelOfRicks.com");
        session.setId(SESSION_ID);

        desc = new CollectionDescription();
        desc.setId("666");

        ReflectionTestUtils.setField(service, "sessionServiceSupplier", sessionServiceSupplier);
        ReflectionTestUtils.setField(service, "userServiceSupplier", userServiceSupplier);
        ReflectionTestUtils.setField(service, "collectionsSupplier", collectionsSupplier);
        ReflectionTestUtils.setField(service, "permissionsServiceSupplier", permissionsServiceSupplier);
        ReflectionTestUtils.setField(service, "serviceStoreSupplier", serviceStoreSupplier);
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnNotAuthorisedIfSessionIDNull() throws Exception {
        try {
            service.identifyUser(null);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(notAuthenticatedEx));
            verifyZeroInteractions(sessionsService, usersService);
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnNotAuthorisedIfSessionIDEmpty() throws Exception {
        try {
            service.identifyUser("");
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(notAuthenticatedEx));
            verifyZeroInteractions(sessionsService, usersService);
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnInternalServerErrorIfErrorGettingSession() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenThrow(new IOException("error getting session"));
        try {
            service.identifyUser(SESSION_ID);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(internalServerErrorEx));
            verify(sessionsService, times(1)).get(SESSION_ID);
            verifyZeroInteractions(usersService);
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnNotAuthenicatedIfSessionNotFound() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(null);
        try {
            service.identifyUser(SESSION_ID);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(notAuthenticatedEx));
            verify(sessionsService, times(1)).get(SESSION_ID);
            verifyZeroInteractions(usersService);
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnNotFoundIfUserDoesNotExist() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);
        when(usersService.exists(session.getEmail()))
                .thenReturn(false);
        try {
            service.identifyUser(SESSION_ID);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(notFoundEx));
            verify(sessionsService, times(1)).get(SESSION_ID);
            verify(usersService, times(1)).exists(session.getEmail());
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnInternalServerErrorIfErrorCheckingUserExistance() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);
        when(usersService.exists(session.getEmail()))
                .thenThrow(new IOException("something terrible happened!"));
        try {
            service.identifyUser(SESSION_ID);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(internalServerErrorEx));
            verify(sessionsService, times(1)).get(SESSION_ID);
            verify(usersService, times(1)).exists(session.getEmail());
            throw ex;
        }
    }

    @Test
    public void shouldReturnUserIdentityIfSessionAndUserExist() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);
        when(usersService.exists(session.getEmail()))
                .thenReturn(true);

        UserIdentity expected = new UserIdentity(session);

        UserIdentity actual = service.identifyUser(SESSION_ID);

        assertThat(actual, equalTo(expected));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(usersService, times(1)).exists(session.getEmail());
    }


    @Test(expected = DatasetPermissionsException.class)
    public void testGetSession_sessionIDNull() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        try {
            serviceImpl.getSession(null);
        } catch (DatasetPermissionsException ex) {
            verify(sessionsService, never()).get(anyString());

            assertThat(ex.statusCode, equalTo(SC_BAD_REQUEST));
            assertThat(ex.getMessage(), equalTo("session ID required but empty"));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetSession_sessionIDEmpty() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        try {
            serviceImpl.getSession("");
        } catch (DatasetPermissionsException ex) {
            verify(sessionsService, never()).get(anyString());

            assertThat(ex.statusCode, equalTo(SC_BAD_REQUEST));
            assertThat(ex.getMessage(), equalTo("session ID required but empty"));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetSession_IOException() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(sessionsService.get("666"))
                .thenThrow(new IOException("BOOOOOOOOM"));

        try {
            serviceImpl.getSession("666");
        } catch (DatasetPermissionsException ex) {
            verify(sessionsService, times(1)).get("666");

            assertThat(ex.statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
            assertThat(ex.getMessage(), equalTo("internal server error"));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetSession_sessionNotFound() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(sessionsService.get("666"))
                .thenReturn(null);

        try {
            serviceImpl.getSession("666");
        } catch (DatasetPermissionsException ex) {
            verify(sessionsService, times(1)).get("666");

            assertThat(ex.statusCode, equalTo(SC_UNAUTHORIZED));
            assertThat(ex.getMessage(), equalTo("session not found"));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetSession_sessionExpired() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(sessionsService.get("666"))
                .thenReturn(session);
        when(sessionsService.expired(session))
                .thenReturn(true);

        try {
            serviceImpl.getSession("666");
        } catch (DatasetPermissionsException ex) {
            verify(sessionsService, times(1)).get("666");
            verify(sessionsService, times(1)).expired(session);

            assertThat(ex.statusCode, equalTo(SC_UNAUTHORIZED));
            assertThat(ex.getMessage(), equalTo("session expired"));
            throw ex;
        }
    }

    @Test
    public void testGetSession_success() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(sessionsService.get("666"))
                .thenReturn(session);
        when(sessionsService.expired(session))
                .thenReturn(false);

        Session s = serviceImpl.getSession("666");

        verify(sessionsService, times(1)).get("666");
        verify(sessionsService, times(1)).expired(session);
        assertThat(s, equalTo(session));
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetCollection_IDNull() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        try {
            serviceImpl.getCollection(null);
        } catch (DatasetPermissionsException ex) {
            verify(collections, never()).getCollection(anyString());

            assertThat(ex.statusCode, equalTo(SC_BAD_REQUEST));
            assertThat(ex.getMessage(), equalTo("collection ID required but was empty"));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetCollection_IOException() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(collections.getCollection("666"))
                .thenThrow(new IOException());

        try {
            serviceImpl.getCollection("666");
        } catch (DatasetPermissionsException ex) {
            verify(collections, times(1)).getCollection("666");

            assertThat(ex.statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
            assertThat(ex.getMessage(), equalTo("internal server error"));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetCollection_collectionNotFound() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(collections.getCollection("666"))
                .thenReturn(null);

        try {
            serviceImpl.getCollection("666");
        } catch (DatasetPermissionsException ex) {
            verify(collections, times(1)).getCollection("666");

            assertThat(ex.statusCode, equalTo(SC_NOT_FOUND));
            assertThat(ex.getMessage(), equalTo("collection not found"));
            throw ex;
        }
    }

    @Test
    public void testGetCollection_success() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(collections.getCollection("666"))
                .thenReturn(collection);

        Collection c = serviceImpl.getCollection("666");

        verify(collections, times(1)).getCollection("666");
        assertThat(c, equalTo(collection));
    }

    @Test
    public void testValidateCollectionContainsRequestedDataset_datasetPresent() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        CollectionDataset ds = new CollectionDataset();
        ds.setId("666");

        CollectionDescription containingDataset = new CollectionDescription();
        containingDataset.addDataset(ds);

        when(collection.getDescription())
                .thenReturn(containingDataset);

        serviceImpl.validateCollectionContainsRequestedDataset(collection, "666");
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testValidateCollectionContainsRequestedDataset_datasetNotPresent() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        CollectionDataset ds = new CollectionDataset();
        ds.setId("666");

        CollectionDescription containingDataset = new CollectionDescription();
        containingDataset.addDataset(ds);

        when(collection.getDescription())
                .thenReturn(containingDataset);

        try {
            serviceImpl.validateCollectionContainsRequestedDataset(collection, "667");
        } catch (DatasetPermissionsException ex) {
            assertThat(ex.getMessage(), equalTo("requested collection does not contain the requested dataset"));
            assertThat(ex.statusCode, equalTo(SC_BAD_REQUEST));
            throw ex;
        }
    }

    @Test
    public void testIsAdminOrPublisher_adminEditorUser() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canEdit(session))
                .thenReturn(true);

        assertThat(serviceImpl.isAdminOrPublisher(session), equalTo(true));

        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testIsAdminOrPublisher_viewerUser() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canEdit(session))
                .thenReturn(false);

        assertThat(serviceImpl.isAdminOrPublisher(session), equalTo(false));

        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testIsAdminOrPublisher_IOException() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canEdit(session))
                .thenThrow(new IOException());

        try {
            serviceImpl.isAdminOrPublisher(session);
        } catch (DatasetPermissionsException ex) {
            verify(permissionsService, times(1)).canEdit(session);

            assertThat(ex.getMessage(), equalTo("internal server error"));
            assertThat(ex.statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
            throw ex;
        }
    }

    @Test
    public void testIsCollectionViewer_success() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canView(session, desc))
                .thenReturn(true);

        boolean canView = serviceImpl.isCollectionViewer(session, desc);

        assertThat(canView, equalTo(true));
        verify(permissionsService, times(1)).canView(session, desc);
    }

    @Test
    public void testIsCollectionViewer_fail() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canView(session, desc))
                .thenReturn(false);

        boolean canView = serviceImpl.isCollectionViewer(session, desc);

        assertThat(canView, equalTo(false));
        verify(permissionsService, times(1)).canView(session, desc);
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testIsCollectionViewer_IOException() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canView(session, desc))
                .thenThrow(new IOException());

        try {
            serviceImpl.isCollectionViewer(session, desc);
        } catch (DatasetPermissionsException ex) {
            verify(permissionsService, times(1)).canView(session, desc);
            assertThat(ex.getMessage(), equalTo("internal server error"));
            assertThat(ex.statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testAssignPermissions_AdminUserPermissionServiceEx() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canEdit(session))
                .thenThrow(new IOException());

        try {
            serviceImpl.assignPermissions(session, desc, "666");
        } catch (DatasetPermissionsException ex) {
            verify(permissionsService, times(1)).canEdit(session);
            assertThat(ex.getMessage(), equalTo("internal server error"));
            assertThat(ex.statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
            throw ex;
        }
    }

    @Test
    public void testAssignPermissions_AdminUserSuccess() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canEdit(session))
                .thenReturn(true);

        DatasetPermissions permissions = serviceImpl.assignPermissions(session, desc, "666");

        assertTrue(permissions.getPermissions().contains(CREATE));
        assertTrue(permissions.getPermissions().contains(READ));
        assertTrue(permissions.getPermissions().contains(UPDATE));
        assertTrue(permissions.getPermissions().contains(DELETE));
        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testAssignPermissions_ViewerUserSuccess() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, desc))
                .thenReturn(true);

        DatasetPermissions permissions = serviceImpl.assignPermissions(session, desc, "666");

        assertTrue(permissions.getPermissions().contains(READ));
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, desc);
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testAssignPermissions_ViewerUserPermissionServiceEx() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, desc))
                .thenThrow(new IOException());

        try {
            serviceImpl.assignPermissions(session, desc, "666");
        } catch (DatasetPermissionsException ex) {
            verify(permissionsService, times(1)).canEdit(session);
            verify(permissionsService, times(1)).canView(session, desc);
            assertThat(ex.getMessage(), equalTo("internal server error"));
            assertThat(ex.statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
            throw ex;
        }
    }

    @Test
    public void testAssignPermissions_ViewerUserNotInTeamAssignedToCollection() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, desc))
                .thenReturn(false);

        DatasetPermissions permissions = serviceImpl.assignPermissions(session, desc, "666");

        assertTrue(permissions.getPermissions().isEmpty());
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, desc);
    }

    @Test
    public void testGetUserPermissions_adminUser() throws Exception {
        when(sessionsService.get("666"))
                .thenReturn(session);
        when(sessionsService.expired(session))
                .thenReturn(false);
        when(collections.getCollection("666"))
                .thenReturn(collection);

        CollectionDataset collectionDataset = new CollectionDataset();
        collectionDataset.setId("666");
        desc.addDataset(collectionDataset);

        when(collection.getDescription())
                .thenReturn(desc);

        when(permissionsService.canEdit(session))
                .thenReturn(true);

        DatasetPermissions permissions = service.getUserPermissions("666", "666", "666");

        assertTrue(permissions.getPermissions().contains(CREATE));
        assertTrue(permissions.getPermissions().contains(READ));
        assertTrue(permissions.getPermissions().contains(UPDATE));
        assertTrue(permissions.getPermissions().contains(DELETE));

        verify(sessionsService, times(1)).get("666");
        verify(sessionsService, times(1)).expired(session);
        verify(collections, times(1)).getCollection("666");
        verify(permissionsService, times(1)).canEdit(session);
        verifyNoMoreInteractions(sessionsService, collections, permissionsService);
    }

    @Test
    public void testGetUserPermissions_viewer() throws Exception {
        when(sessionsService.get("666"))
                .thenReturn(session);
        when(sessionsService.expired(session))
                .thenReturn(false);
        when(collections.getCollection("666"))
                .thenReturn(collection);

        CollectionDataset collectionDataset = new CollectionDataset();
        collectionDataset.setId("666");
        CollectionDescription description = new CollectionDescription();
        description.addDataset(collectionDataset);

        when(collection.getDescription())
                .thenReturn(description);

        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, description))
                .thenReturn(true);

        DatasetPermissions permissions = service.getUserPermissions("666", "666", "666");

        assertTrue(permissions.getPermissions().contains(READ));

        verify(sessionsService, times(1)).get("666");
        verify(sessionsService, times(1)).expired(session);
        verify(collections, times(1)).getCollection("666");
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, description);
        verifyNoMoreInteractions(sessionsService, collections, permissionsService);
    }

    @Test
    public void testGetServiceAccountSuccess() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(serviceStore.get("666"))
                .thenReturn(serviceAccount);

        ServiceAccount account = serviceImpl.getServiceAccount("666");

        assertThat(account, equalTo(serviceAccount));
        verify(serviceStore, times(1)).get("666");
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetServiceAccount_IOException() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(serviceStore.get("666"))
                .thenThrow(new IOException());

        try {
            serviceImpl.getServiceAccount("666");
        } catch (DatasetPermissionsException ex) {
            verify(serviceStore, times(1)).get("666");
            assertThat(ex.statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
            assertThat(ex.getMessage(), equalTo("internal server error"));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetServiceAccount_NotFound() throws Exception {
        AuthorisationServiceImpl serviceImpl = (AuthorisationServiceImpl) service;

        when(serviceStore.get("666"))
                .thenReturn(null);

        try {
            serviceImpl.getServiceAccount("666");
        } catch (DatasetPermissionsException ex) {
            verify(serviceStore, times(1)).get("666");
            assertThat(ex.statusCode, equalTo(SC_UNAUTHORIZED));
            assertThat(ex.getMessage(), equalTo("permisson denied service account not found"));
            throw ex;
        }
    }

    @Test
    public void testGetServicePermissionsSuccess() throws Exception {
        when(serviceStore.get("666"))
                .thenReturn(serviceAccount);
        when(serviceAccount.getId())
                .thenReturn("Über-service");

        DatasetPermissions permissions = service.getServicePermissions("666");

        assertTrue(permissions.getPermissions().contains(CREATE));
        assertTrue(permissions.getPermissions().contains(READ));
        assertTrue(permissions.getPermissions().contains(UPDATE));
        assertTrue(permissions.getPermissions().contains(DELETE));

        verify(serviceStore, times(1)).get("666");
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetServicePermissions_IOException() throws Exception {
        when(serviceStore.get("666"))
                .thenThrow(new IOException());

        try {
            service.getServicePermissions("666");
        } catch (DatasetPermissionsException ex) {
            verify(serviceStore, times(1)).get("666");
            assertThat(ex.statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
            assertThat(ex.getMessage(), equalTo("internal server error"));
            throw ex;
        }
    }

    @Test(expected = DatasetPermissionsException.class)
    public void testGetServicePermissions_NotFound() throws Exception {
        when(serviceStore.get("666"))
                .thenReturn(null);

        try {
            service.getServicePermissions("666");
        } catch (DatasetPermissionsException ex) {
            verify(serviceStore, times(1)).get("666");
            assertThat(ex.statusCode, equalTo(SC_UNAUTHORIZED));
            assertThat(ex.getMessage(), equalTo("permisson denied service account not found"));
            throw ex;
        }
    }

}
