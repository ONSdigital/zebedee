package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.HEAD;
import java.io.IOException;
import java.util.HashSet;

import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserInstanceCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserNone;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceAccountNotFoundException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceTokenNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionNotFoundException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


public class CMDPermissionsServiceImplTest {

    static final String SESSION_ID = "217"; // Overlook Hotel room ...
    static final String SERVICE_TOKEN = "Union_Aerospace_Corporation"; // DOOM \m/
    static final String DATASET_ID = "Ohhh get schwifty";
    static final String COLLECTION_ID = "666";

    @Mock
    private Collections collectionsService;

    @Mock
    private Sessions sessions;

    @Mock
    private ServiceStore serviceStore;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private Session session;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    private CMDPermissionsServiceImpl service;
    private CollectionDataset dataset;
    private HashSet datasets;
    private CRUD none;
    private CRUD fullPermissions;
    private CRUD readOnly;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        dataset = new CollectionDataset();
        dataset.setId(DATASET_ID);
        datasets = new HashSet<CollectionDataset>() {{
            add(dataset);
        }};

        when(collection.getDescription()).thenReturn(description);

        service = new CMDPermissionsServiceImpl(collectionsService, serviceStore, permissionsService);

        none = new CRUD();
        fullPermissions = new CRUD().permit(CREATE, READ, UPDATE, DELETE);
        readOnly = new CRUD().permit(READ);
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_tokenNull() throws Exception {
        try {
            service.getServiceAccountByID(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyNoInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_tokenEmpty() throws Exception {
        try {
            service.getServiceAccountByID("");
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyNoInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_IOException() throws Exception {
        when(serviceStore.get(SERVICE_TOKEN))
                .thenThrow(new IOException(""));

        try {
            service.getServiceAccountByID(SERVICE_TOKEN);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_notFound() throws Exception {
        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(null);

        try {
            service.getServiceAccountByID(SERVICE_TOKEN);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test
    public void testGetServiceAccount_success() throws Exception {
        ServiceAccount expected = new ServiceAccount(SERVICE_TOKEN);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(expected);

        ServiceAccount actual = service.getServiceAccountByID(SERVICE_TOKEN);
        assertThat(actual, equalTo(expected));
        verify(serviceStore, times(1)).get(SERVICE_TOKEN);
    }

    @Test
    public void testCollectionContainsDataset_false() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        when(description.getDatasets())
                .thenReturn(new HashSet<>());

        assertFalse(service.collectionContainsDataset(collection, DATASET_ID));
    }

    @Test
    public void testCollectionContainsDataset_true() throws Exception {
        CollectionDataset dataset = new CollectionDataset();
        dataset.setId(DATASET_ID);

        HashSet datasets = new HashSet<CollectionDataset>() {{
            add(dataset);
        }};

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getDatasets())
                .thenReturn(datasets);

        assertTrue(service.collectionContainsDataset(collection, DATASET_ID));
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_idNull() throws Exception {
        try {
            service.getCollectionByID(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyNoInteractions(collectionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_idEmpty() throws Exception {
        try {
            service.getCollectionByID("");
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyNoInteractions(collectionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_IOException() throws Exception {
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenThrow(new IOException());

        try {
            service.getCollectionByID(COLLECTION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_notFOund() throws Exception {
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(null);

        try {
            service.getCollectionByID(COLLECTION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_NOT_FOUND));
            verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
            throw ex;
        }
    }

    @Test
    public void testGetCollection_success() throws Exception {
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(collection);

        Collection actual = service.getCollectionByID(COLLECTION_ID);
        assertThat(actual, equalTo(collection));
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_tokenNull() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, null, DATASET_ID, null);
        try {
            service.getServiceDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyNoInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_tokenEmpty() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, "", DATASET_ID, null);
        try {
            service.getServiceDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyNoInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_notFound() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, DATASET_ID, null);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(null);

        try {
            service.getServiceDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_IOException() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, DATASET_ID, null);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenThrow(new IOException());

        try {
            service.getServiceDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test
    public void testGetServiceDatasetPermissions_success() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, DATASET_ID, null);

        ServiceAccount expected = new ServiceAccount(SERVICE_TOKEN);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(expected);

        CRUD result = service.getServiceDatasetPermissions(request);

        assertThat(result, equalTo(fullPermissions));
        verify(serviceStore, times(1)).get(SERVICE_TOKEN);
    }

    @Test
    public void testGetUserDatasetPermissions_editorUser() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(session, SERVICE_TOKEN, DATASET_ID, COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(true);

        CRUD actual = service.getUserDatasetPermissions(request);

        assertThat(actual, equalTo(fullPermissions));
        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testGetUserDatasetPermissions_viewerUserAssignedToCollection() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(session, SERVICE_TOKEN, DATASET_ID, COLLECTION_ID);

        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(collection);
        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, COLLECTION_ID))
                .thenReturn(true);
        when(description.getDatasets())
                .thenReturn(datasets);

        CRUD actual = service.getUserDatasetPermissions(request);

        assertThat(actual, equalTo(readOnly));
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, COLLECTION_ID);
    }

    @Test
    public void testGetUserDatasetPermissions_viewerCannotViewCollection() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(session, SERVICE_TOKEN, DATASET_ID, COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, COLLECTION_ID))
                .thenReturn(false);

        CRUD actual = service.getUserDatasetPermissions(request);

        assertThat(actual, equalTo(none));
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, COLLECTION_ID);
    }

    @Test
    public void testGetUserDatasetPermissions_viewerDatasetNotInCollection() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(session, SERVICE_TOKEN, DATASET_ID, COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, COLLECTION_ID))
                .thenReturn(true);
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(collection);
        when(description.getDatasets())
                .thenReturn(new HashSet<>());

        CRUD actual = service.getUserDatasetPermissions(request);

        assertThat(actual, equalTo(none));
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, COLLECTION_ID);
    }

    @Test
    public void testUserHasEditCollectionPermission_true() throws Exception {
        when(permissionsService.canEdit(session))
                .thenReturn(true);

        assertTrue(service.userHasEditCollectionPermission(session));

        verify(permissionsService, times(1))
                .canEdit(session);
    }

    @Test
    public void testUserHasEditCollectionPermission_false() throws Exception {
        when(permissionsService.canEdit(session))
                .thenReturn(false);

        assertFalse(service.userHasEditCollectionPermission(session));

        verify(permissionsService, times(1))
                .canEdit(session);
    }

    @Test(expected = PermissionsException.class)
    public void testUserHasEditCollectionPermission_IOException() throws Exception {
        when(permissionsService.canEdit(session))
                .thenThrow(new IOException());

        try {
            service.userHasEditCollectionPermission(session);
        } catch (PermissionsException ex) {
            verify(permissionsService, times(1))
                    .canEdit(session);
            PermissionsException expected = internalServerErrorException();
            assertThat(expected.statusCode, equalTo(ex.statusCode));
            assertThat(expected.getMessage(), equalTo(ex.getMessage()));
            throw ex;
        }
    }

    @Test
    public void testUserHasViewCollectionPermission_true() throws Exception {
        when(permissionsService.canView(session, COLLECTION_ID))
                .thenReturn(true);

        assertTrue(service.userHasViewCollectionPermission(session, COLLECTION_ID));

        verify(permissionsService, times(1))
                .canView(session, COLLECTION_ID);
    }

    @Test
    public void testUserHasViewCollectionPermission_false() throws Exception {
        when(permissionsService.canView(session, COLLECTION_ID))
                .thenReturn(false);

        assertFalse(service.userHasViewCollectionPermission(session, COLLECTION_ID));

        verify(permissionsService, times(1))
                .canView(session, COLLECTION_ID);
    }

    @Test(expected = PermissionsException.class)
    public void testUserHasViewCollectionPermission_IOException() throws Exception {
        when(permissionsService.canView(session, COLLECTION_ID))
                .thenThrow(new IOException());

        try {
            service.userHasViewCollectionPermission(session, COLLECTION_ID);
        } catch (PermissionsException ex) {
            verify(permissionsService, times(1))
                    .canView(session, COLLECTION_ID);
            PermissionsException expected = internalServerErrorException();
            assertThat(expected.statusCode, equalTo(ex.statusCode));
            assertThat(expected.getMessage(), equalTo(ex.getMessage()));
            throw ex;
        }
    }

    @Test
    public void testGetUserInstancePermissions_publisherUserSuccess() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(session, null, null, null);

        when(permissionsService.canEdit(session)).thenReturn(true);

        CRUD expected = grantUserInstanceCreateReadUpdateDelete(request);
        CRUD actual = service.getUserInstancePermissions(request);

        verify(permissionsService, times(1)).canEdit(session);
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testGetUserInstancePermissions_viewerUserSuccess() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(session, null, null, null);

        when(permissionsService.canEdit(session)).thenReturn(false);

        CRUD expected = grantUserNone(request, "");
        CRUD actual = service.getUserInstancePermissions(request);

        verify(permissionsService, times(1)).canEdit(session);
        assertThat(actual, equalTo(expected));
    }

    @Test(expected = PermissionsException.class)
    public void testGetUserInstancePermissions_requestIsNull() throws Exception {
        try {
            service.getUserInstancePermissions(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verifyNoInteractions(serviceStore, collectionsService, collectionsService, sessions);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetUserInstancePermissions_sessionNotFound() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, null, null, null);

        try {
            service.getUserInstancePermissions(request);
        } catch (PermissionsException ex) {
            PermissionsException expected = sessionNotFoundException();

            assertThat(ex.getMessage(), equalTo(expected.getMessage()));
            assertThat(ex.statusCode, equalTo(expected.statusCode));

            verifyNoInteractions(serviceStore, collectionsService, collectionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetUserInstancePermissions_canEditIOEx() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(session, null, null, null);

        when(permissionsService.canEdit(session)).thenThrow(new IOException());

        PermissionsException expected = internalServerErrorException();
        try {
            CRUD actual = service.getUserInstancePermissions(request);
        } catch (PermissionsException ex) {
            verify(permissionsService, times(1)).canEdit(session);


            assertThat(ex.statusCode, equalTo(expected.statusCode));
            assertThat(ex.getMessage(), equalTo(expected.getMessage()));
            throw ex;
        }
    }

    /**
     * Test {@link CMDPermissionsService#getServiceInstancePermissions(GetPermissionsRequest)} for scenrario where a
     * null {@link GetPermissionsRequest} is provided.
     */
    @Test(expected = PermissionsException.class)
    public void testGetServiceInstancePermissions_requestNull() throws PermissionsException {
        try {
            service.getServiceInstancePermissions(null);
        } catch (PermissionsException ex) {
            PermissionsException expected = internalServerErrorException();

            assertThat(ex.getMessage(), equalTo(expected.getMessage()));
            assertThat(ex.statusCode, equalTo(expected.statusCode));

            verifyNoInteractions(serviceStore, collectionsService, collectionsService, sessions);
            throw ex;
        }
    }

    /**
     * Test {@link CMDPermissionsService#getServiceInstancePermissions(GetPermissionsRequest)} for scenario where a
     * null {@link ServiceAccount#id} is provided.
     */
    @Test(expected = PermissionsException.class)
    public void testGetServiceInstancePermissions_serviceTokenEmpty() throws PermissionsException {
        GetPermissionsRequest request = new GetPermissionsRequest(null, null, null, null);

        try {
            service.getServiceInstancePermissions(request);
        } catch (PermissionsException ex) {
            PermissionsException expected = serviceTokenNotProvidedException();

            assertThat(ex.getMessage(), equalTo(expected.getMessage()));
            assertThat(ex.statusCode, equalTo(expected.statusCode));

            verifyNoInteractions(serviceStore, collectionsService, collectionsService, sessions);
            throw ex;
        }
    }

    /**
     * Test {@link CMDPermissionsService#getServiceInstancePermissions(GetPermissionsRequest)} for scenario where
     * {@link ServiceStore#get(String)} throws an {@link IOException}.
     */
    @Test(expected = PermissionsException.class)
    public void testGetServiceInstancePermissions_serviceStoreIOEx() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, null, null);

        when(serviceStore.get(SERVICE_TOKEN)).thenThrow(new IOException());

        try {
            service.getServiceInstancePermissions(request);
        } catch (PermissionsException ex) {
            PermissionsException expected = internalServerErrorException();

            assertThat(ex.getMessage(), equalTo(expected.getMessage()));
            assertThat(ex.statusCode, equalTo(expected.statusCode));

            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            verifyNoInteractions(collectionsService, collectionsService, sessions);
            throw ex;
        }
    }

    /**
     * Test {@link CMDPermissionsService#getServiceInstancePermissions(GetPermissionsRequest)} for scenario where the
     * no {@link ServiceAccount} is found for the provided service account ID.
     */
    @Test(expected = PermissionsException.class)
    public void testGetServiceInstancePermissions_serviceAccountNotFound() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, null, null);

        when(serviceStore.get(SERVICE_TOKEN)).thenReturn(null);

        try {
            service.getServiceInstancePermissions(request);
        } catch (PermissionsException ex) {
            PermissionsException expected = serviceAccountNotFoundException();

            assertThat(ex.getMessage(), equalTo(expected.getMessage()));
            assertThat(ex.statusCode, equalTo(expected.statusCode));

            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            verifyNoInteractions(collectionsService, collectionsService, sessions);
            throw ex;
        }
    }

    /**
     * Test {@link CMDPermissionsService#getServiceInstancePermissions(GetPermissionsRequest)} success case. Given a
     * valid request the service returns {@link CRUD} permissed granted to the {@link ServiceAccount}
     */
    @Test
    public void testGetServiceInstancePermissions_success() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, null, null);
        ServiceAccount account = new ServiceAccount(SERVICE_TOKEN);
        CRUD expected = new CRUD().permit(CREATE, READ, UPDATE, DELETE);

        when(serviceStore.get(SERVICE_TOKEN)).thenReturn(account);

        CRUD actual = service.getServiceInstancePermissions(request);

        assertThat(actual, equalTo(expected));

        verify(serviceStore, times(1)).get(SERVICE_TOKEN);
        verifyNoInteractions(collectionsService, collectionsService, sessions);
    }
}
