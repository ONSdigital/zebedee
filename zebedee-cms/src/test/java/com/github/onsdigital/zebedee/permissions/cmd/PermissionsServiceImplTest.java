package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PermissionsServiceImplTest {

    static final String SESSION_ID = "217"; // Overlook Hotel room ...
    static final String SERVICE_TOKEN = "Union_Aerospace_Corporation"; // DOOM \m/
    static final String DATASET_ID = "Ohhh get schwifty";
    static final String COLLECTION_ID = "666";

    @Mock
    private Collections collectionsService;

    @Mock
    private SessionsService sessionsService;

    @Mock
    private ServiceStore serviceStore;

    @Mock
    private CollectionPermissionsService collectionPermissionsService;

    @Mock
    private Session session;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    private PermissionsServiceImpl service;
    private CollectionDataset dataset;
    private HashSet datasets;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        dataset = new CollectionDataset();
        dataset.setId(DATASET_ID);
        datasets = new HashSet<CollectionDataset>() {{
            add(dataset);
        }};

        when(collection.getDescription()).thenReturn(description);

        service = new PermissionsServiceImpl(sessionsService, collectionsService, serviceStore,
                collectionPermissionsService);
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_sessionIDNull() throws Exception {
        try {
            service.getSession(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));

            verifyZeroInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_sessionNotFound() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(null);

        try {
            service.getSession(SESSION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verifyNoMoreInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_IOException() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenThrow(new IOException(""));

        try {
            service.getSession(SESSION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verifyNoMoreInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_expired() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);

        when(sessionsService.expired(session))
                .thenReturn(true);

        try {
            service.getSession(SESSION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verify(sessionsService, times(1)).expired(session);
            throw ex;
        }
    }

    @Test
    public void testGetSession_success() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);

        when(sessionsService.expired(session))
                .thenReturn(false);

        Session result = service.getSession(SESSION_ID);
        assertThat(result, equalTo(session));

        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_tokenNull() throws Exception {
        try {
            service.getServiceAccount(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_tokenEmpty() throws Exception {
        try {
            service.getServiceAccount("");
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_IOException() throws Exception {
        when(serviceStore.get(SERVICE_TOKEN))
                .thenThrow(new IOException(""));

        try {
            service.getServiceAccount(SERVICE_TOKEN);
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
            service.getServiceAccount(SERVICE_TOKEN);
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

        ServiceAccount actual = service.getServiceAccount(SERVICE_TOKEN);
        assertThat(actual, equalTo(expected));
        verify(serviceStore, times(1)).get(SERVICE_TOKEN);
    }

    @Test
    public void testIsDatasetInCollection_false() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        when(description.getDatasets())
                .thenReturn(new HashSet<>());

        assertFalse(service.isDatasetInCollection(collection, DATASET_ID));
    }

    @Test
    public void testIsDatasetInCollection_true() throws Exception {
        CollectionDataset dataset = new CollectionDataset();
        dataset.setId(DATASET_ID);

        HashSet datasets = new HashSet<CollectionDataset>() {{
            add(dataset);
        }};

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getDatasets())
                .thenReturn(datasets);

        assertTrue(service.isDatasetInCollection(collection, DATASET_ID));
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_idNull() throws Exception {
        try {
            service.getCollection(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(collectionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_idEmpty() throws Exception {
        try {
            service.getCollection("");
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(collectionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_IOException() throws Exception {
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenThrow(new IOException());

        try {
            service.getCollection(COLLECTION_ID);
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
            service.getCollection(COLLECTION_ID);
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

        Collection actual = service.getCollection(COLLECTION_ID);
        assertThat(actual, equalTo(collection));
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_tokenNull() throws Exception {
        try {
            service.getServiceDatasetPermissions(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_tokenEmpty() throws Exception {
        try {
            service.getServiceDatasetPermissions(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_notFound() throws Exception {
        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(null);

        try {
            service.getServiceDatasetPermissions(SERVICE_TOKEN);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_IOException() throws Exception {
        when(serviceStore.get(SERVICE_TOKEN))
                .thenThrow(new IOException());

        try {
            service.getServiceDatasetPermissions(SERVICE_TOKEN);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test
    public void testGetServiceDatasetPermissions_success() throws Exception {
        ServiceAccount expected = new ServiceAccount(SERVICE_TOKEN);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(expected);

        CRUD result = service.getServiceDatasetPermissions(SERVICE_TOKEN);

        assertThat(result, equalTo(CRUD.permitCreateReadUpdateDelete()));
        verify(serviceStore, times(1)).get(SERVICE_TOKEN);
    }

    @Test
    public void testGetUserDatasetPermissions_editorUser() throws Exception {
        when(sessionsService.get(SESSION_ID)).thenReturn(session);
        when(collectionPermissionsService.hasEdit(session)).thenReturn(true);

        CRUD actual = service.getUserDatasetPermissions(SESSION_ID, DATASET_ID, COLLECTION_ID);

        assertThat(actual, equalTo(CRUD.permitCreateReadUpdateDelete()));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
        verify(collectionPermissionsService, times(1)).hasEdit(session);
    }

    @Test
    public void testGetUserDatasetPermissions_viewerUserAssignedToCollection() throws Exception {
        when(sessionsService.get(SESSION_ID)).thenReturn(session);
        when(collectionsService.getCollection(COLLECTION_ID)).thenReturn(collection);
        when(collectionPermissionsService.hasEdit(session)).thenReturn(false);
        when(collectionPermissionsService.hasView(session, description)).thenReturn(true);
        when(description.getDatasets()).thenReturn(datasets);

        CRUD actual = service.getUserDatasetPermissions(SESSION_ID, DATASET_ID, COLLECTION_ID);

        assertThat(actual, equalTo(CRUD.permitRead()));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
        verify(collectionPermissionsService, times(1)).hasEdit(session);
        verify(collectionPermissionsService, times(1)).hasView(session, description);
    }

    @Test(expected = PermissionsException.class)
    public void testGetUserDatasetPermissions_viewerCollectionIDNull() throws Exception {
        when(sessionsService.get(SESSION_ID)).thenReturn(session);
        when(collectionPermissionsService.hasEdit(session)).thenReturn(false);

        try {
            service.getUserDatasetPermissions(SESSION_ID, null, null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verify(sessionsService, times(1)).get(SESSION_ID);
            verify(sessionsService, times(1)).expired(session);
            verify(collectionsService, never()).getCollection(anyString());
            verify(collectionPermissionsService, times(1)).hasEdit(session);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetUserDatasetPermissions_viewerDatasetIDNull() throws Exception {
        when(sessionsService.get(SESSION_ID)).thenReturn(session);
        when(collectionPermissionsService.hasEdit(session)).thenReturn(false);
        when(collectionsService.getCollection(COLLECTION_ID)).thenReturn(collection);

        try {
            service.getUserDatasetPermissions(SESSION_ID, null, COLLECTION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verify(sessionsService, times(1)).get(SESSION_ID);
            verify(sessionsService, times(1)).expired(session);
            verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
            verify(collectionPermissionsService, times(1)).hasEdit(session);
            throw ex;
        }
    }

    @Test
    public void testGetUserDatasetPermissions_viewerCannotViewCollection() throws Exception {
        when(sessionsService.get(SESSION_ID)).thenReturn(session);
        when(collectionPermissionsService.hasEdit(session)).thenReturn(false);
        when(collectionPermissionsService.hasView(session, description)).thenReturn(false);
        when(collectionsService.getCollection(COLLECTION_ID)).thenReturn(collection);

        CRUD actual = service.getUserDatasetPermissions(SESSION_ID, DATASET_ID, COLLECTION_ID);

        assertThat(actual, equalTo(CRUD.permitNone()));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
        verify(collectionPermissionsService, times(1)).hasEdit(session);
        verify(collectionPermissionsService, times(1)).hasView(session, description);
    }

    @Test
    public void testGetUserDatasetPermissions_viewerDatasetNotInCollection() throws Exception {
        when(sessionsService.get(SESSION_ID)).thenReturn(session);
        when(collectionPermissionsService.hasEdit(session)).thenReturn(false);
        when(collectionPermissionsService.hasView(session, description)).thenReturn(true);
        when(collectionsService.getCollection(COLLECTION_ID)).thenReturn(collection);
        when(description.getDatasets()).thenReturn(new HashSet<>());

        CRUD actual = service.getUserDatasetPermissions(SESSION_ID, DATASET_ID, COLLECTION_ID);

        assertThat(actual, equalTo(CRUD.permitNone()));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
        verify(collectionPermissionsService, times(1)).hasEdit(session);
        verify(collectionPermissionsService, times(1)).hasView(session, description);
    }
}
