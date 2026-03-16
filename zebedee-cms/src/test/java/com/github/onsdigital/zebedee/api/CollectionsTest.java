package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.DatasetService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CollectionsTest {

    private ZebedeeCmsService mockZebedeeCmsService = mock(ZebedeeCmsService.class);
    private DatasetService mockDatasetService = mock(DatasetService.class);
    private ServletInputStream mockServletInputStream = mock(ServletInputStream.class);

    private Collection mockCollection = mock(Collection.class);
    private CollectionType mockCollectionType = CollectionType.manual;
    private CollectionDescription mockCollectionDescription = mock(CollectionDescription.class);

    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private Session session = mock(Session.class);

    private Collections collections = new Collections(mockZebedeeCmsService, mockDatasetService, true);
    private String collectionID = "123";
    private String resourceID = "345";
    private String edition = "2014";
    private String version = "1";
    private String user = "test@email.com";

    @Before
    public void setUp() throws Exception {
        when(mockZebedeeCmsService.getCollection(collectionID)).thenReturn(mockCollection);
        when(mockCollection.getId()).thenReturn(collectionID);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockCollectionDescription.getType()).thenReturn(mockCollectionType);
    }

    @Test
    public void testPut_DatasetImportDisabled() throws Exception {
        collections = new Collections(mockZebedeeCmsService, mockDatasetService, false);

        // When the put method is called
        collections.put(request, response);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verifyNoInteractions(mockZebedeeCmsService, mockDatasetService);
    }

    @Test
    public void testDelete_DatasetImportDisabled() throws Exception {
        collections = new Collections(mockZebedeeCmsService, mockDatasetService, false);

        // When the put method is called
        collections.delete(request, response);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verifyNoInteractions(mockZebedeeCmsService, mockDatasetService);
    }

    @Test
    public void testPut_Forbidden() throws Exception {

        // Given a PUT request with a valid URL
        String url = String.format("/collections/%s/anything/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, false, mockCollectionType);

        // When the put method is called
        collections.put(request, response);

        // a HTTP 403 is set on the response
        verify(response).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testPut_NoType() throws Exception {

        // Given a PUT request with a valid URL
        String url = String.format("/collections/%s/datasets/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);
        when(mockCollection.getDescription()).thenReturn(null);

        shouldAuthorise(request, true, mockCollectionType);

        // When the put method is called
        collections.put(request, response);

        // a HTTP 500 is set on the response
        verify(response).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testDelete_Forbidden() throws Exception {

        // Given a delete request with a valid URL
        String url = String.format("/collections/%s/anything/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, false, mockCollectionType);

        // When the delete method is called
        collections.delete(request, response);

        // a HTTP 403 is set on the response
        verify(response).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testDelete_NoType() throws Exception {

        // Given a DELETE request with a valid URL
        String url = String.format("/collections/%s/datasets/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);
        when(mockCollection.getDescription()).thenReturn(null);

        shouldAuthorise(request, true, mockCollectionType);

        // When the delete method is called
        collections.delete(request, response);

        // a HTTP 500 is set on the response
        verify(response).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testPut_CollectionNotFound() throws Exception {

        // Given a collection ID that does not exist
        when(mockZebedeeCmsService.getCollection(collectionID)).thenReturn(null);

        String url = String.format("/collections/%s/anything/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);
        shouldAuthorise(request, true, mockCollectionType);

        // When the delete method is called
        collections.put(request, response);

        // The expected response code is set on the response
        verify(response, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testDelete_CollectionNotFound() throws Exception {

        // Given a collection ID that does not exist
        when(mockZebedeeCmsService.getCollection(collectionID)).thenReturn(null);

        String url = String.format("/collections/%s/anything/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);
        shouldAuthorise(request, true, mockCollectionType);

        // When the delete method is called
        collections.delete(request, response);

        // The expected response code is set on the response
        verify(response, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testPut_SessionNotFound() throws Exception {
        // Given a session that does not exist
        when(mockZebedeeCmsService.getSession()).thenReturn(null);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(response, times(1)).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testDelete_SessionNotFound() throws Exception {

        // Given a session that does not exist
        when(mockZebedeeCmsService.getSession()).thenReturn(null);

        // When the delete method is called
        collections.delete(request, response);

        // The expected response code is set on the response
        verify(response, times(1)).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testPutReturnBadRequestURLSegments() throws Exception {

        shouldAuthorise(request, true, mockCollectionType);

        // Given a PUT request with a URL that contains less than the expected 4 segments
        String url = "/collections/123/anything";
        when(request.getPathInfo()).thenReturn(url);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is not called
        verifyNoInteractions(mockDatasetService);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testDeleteReturnBadRequestURLSegments() throws Exception {

        shouldAuthorise(request, true, mockCollectionType);

        // Given a PUT request with a URL that contains less than the expected number of segments
        String url = "/collections/123/anything";

        when(request.getPathInfo()).thenReturn(url);

        // When the delete method is called
        collections.delete(request, response);

        // The dataset service is not called
        verifyNoInteractions(mockDatasetService);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testPut_ReturnBadRequest_NotValidEndpoint() throws Exception {

        shouldAuthorise(request, true, mockCollectionType);

        // Given a PUT request with a URL that contains something other than /collections/{}/{datasets|interactives}/{}
        String url = "/collections/123/anything/345";
        when(request.getPathInfo()).thenReturn(url);

        // When the put method is called
        collections.put(request, response);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testDeleteReturnBadRequestNotValidEndpoint() throws Exception {

        shouldAuthorise(request, true, CollectionType.manual);

        // Given a PUT request with a URL that contains something other than /collections/{}/{datasets|interactives}/{}
        String url = "/collections/123/anything/345";
        when(request.getPathInfo()).thenReturn(url);

        // When the delete method is called
        collections.delete(request, response);

        // The dataset service is not called
        verifyNoInteractions(mockDatasetService);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testPutDataset_EmptyJSON() throws Exception {

        // Given a PUT request with a bad json input
        String url = String.format("/collections/%s/datasets/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);
        when(session.getEmail()).thenReturn(user);

        mockRequestBody("");

        shouldAuthorise(request, true, mockCollectionType);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).updateDatasetInCollection(mockCollection, resourceID, null, user);
    }

    @Test
    public void testPutDataset() throws Exception {

        // Given a PUT request with a valid URL for a dataset
        String url = String.format("/collections/%s/datasets/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);
        when(session.getEmail()).thenReturn(user);

        String json = "{ \"state\": \"inProgress\"}";
        mockRequestBody(json);

        shouldAuthorise(request, true, mockCollectionType);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).updateDatasetInCollection(mockCollection, resourceID, new CollectionDataset(), user);
    }

    @Test
    public void testPutDatasetInvalidPathSize() throws Exception {

        // Given a PUT request with a valid URL for a dataset
        String url = String.format("/collections/%s/datasets/%s/more", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);
        when(session.getEmail()).thenReturn(user);

        String json = "{ \"state\": \"inProgress\"}";
        mockRequestBody(json);

        shouldAuthorise(request, true, mockCollectionType);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is not called
        verifyNoInteractions(mockDatasetService);

        // Then a HTTP 404 is set on the response
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testPutDatasetVersion() throws Exception {

        // Given a PUT request with a valid URL for a dataset
        String url = String.format("/collections/%s/datasets/%s/editions/%s/versions/%s",
                collectionID, resourceID, edition, version);
        when(request.getPathInfo()).thenReturn(url);
        when(session.getEmail()).thenReturn(user);

        String json = "{ \"state\": \"inProgress\"}";
        mockRequestBody(json);

        shouldAuthorise(request, true, mockCollectionType);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).updateDatasetVersionInCollection(
                mockCollection, resourceID, edition, version, new CollectionDatasetVersion(), user);
    }

    @Test
    public void testDeleteDataset() throws Exception {

        // Given a DELETE request with a valid URL
        String url = String.format("/collections/%s/datasets/%s", collectionID, resourceID);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, true, mockCollectionType);

        // When the delete method is called
        collections.delete(request, response);

        // Then a HTTP 204 is set on the response
        verify(response).setStatus(HttpStatus.SC_NO_CONTENT);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).removeDatasetFromCollection(mockCollection, resourceID);
        verifyNoMoreInteractions(mockDatasetService);
    }

    @Test
    public void testDeleteDatasetVersion() throws Exception {

        // Given a DELETE request with a valid URL
        String url = String.format("/collections/%s/datasets/%s/editions/%s/versions/%s",
                collectionID, resourceID, edition, version);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, true, mockCollectionType);

        // When the delete method is called
        collections.delete(request, response);

        // Then a HTTP 204 is set on the response
        verify(response).setStatus(HttpStatus.SC_NO_CONTENT);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).removeDatasetVersionFromCollection(
                mockCollection, resourceID, edition, version);

        verifyNoMoreInteractions(mockDatasetService);
    }

    @Test
    public void testDeleteDatasetVersionInvalidPathSize() throws Exception {

        // Given a DELETE request with a valid URL
        String url = String.format("/collections/%s/datasets/%s/editions/%s/versions",
                collectionID, resourceID, edition);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, true, mockCollectionType);

        // When the delete method is called
        collections.delete(request, response);

        // Then a HTTP 404 is set on the response
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);

        // The dataset service is not called
        verifyNoInteractions(mockDatasetService);
    }

    // mock the authorisation for the given request to authorise the request is the authorise param is true.
    private void shouldAuthorise(HttpServletRequest request, boolean authorise, CollectionType collectionType) throws ZebedeeException, IOException {

        when(mockZebedeeCmsService.getSession()).thenReturn(session);

        PermissionsService permissionsService = mock(PermissionsService.class);
        when(mockZebedeeCmsService.getPermissions()).thenReturn(permissionsService);

        when(permissionsService.canEdit(session)).thenReturn(authorise);
        when(permissionsService.canEdit(session, collectionType)).thenReturn(authorise);
    }

    private void mockRequestBody(String json) throws IOException {

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes())) {

            // needed this monstrosity to mock the request body input stream.
            when(mockServletInputStream.read(any(), anyInt(), anyInt())).thenAnswer(invocationOnMock -> {
                Object[] args = invocationOnMock.getArguments();
                byte[] output = (byte[]) args[0];
                int offset = (int) args[1];
                int length = (int) args[2];
                return byteArrayInputStream.read(output, offset, length);
            });

            when(request.getInputStream()).thenReturn(mockServletInputStream);
        }
    }
}
