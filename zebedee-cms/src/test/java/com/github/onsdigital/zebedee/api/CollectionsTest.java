package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.DatasetService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.http.HttpStatus;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CollectionsTest {

    private ZebedeeCmsService mockZebedee = mock(ZebedeeCmsService.class);
    private DatasetService mockDatasetService = mock(DatasetService.class);

    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);

    private Collections collections = new Collections(mockZebedee, mockDatasetService);
    private String collectionID = "123";
    private String instanceID = "345";

    @Test
    public void TestPut() throws Exception {

        // Given a PUT request with a valid URL
        String url = String.format("/collections/%s/instances/%s", collectionID, instanceID);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, true);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).addInstanceToCollection(collectionID, instanceID);
    }

    @Test
    public void TestPut_Forbidden() throws Exception {

        // Given a PUT request with a valid URL
        String url = String.format("/collections/%s/instances/%s", collectionID, instanceID);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, false);

        // When the put method is called
        collections.put(request, response);

        // a HTTP 403 is set on the response
        verify(response).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void TestPut_ReturnBadRequest_URLSegments() throws Exception {

        shouldAuthorise(request, true);

        // Given a PUT request with a URL that contains less than the expected number of segments
        String url = "/collections/123/instances";
        when(request.getPathInfo()).thenReturn(url);

        // When the put method is called
        collections.put(request, response);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void TestPut_ReturnBadRequest_NotInstanceEndpoint() throws Exception {

        shouldAuthorise(request, true);

        // Given a PUT request with a URL that contains something other than /collections/{}/instances/{}
        String url = "/collections/123/datasets/345";
        when(request.getPathInfo()).thenReturn(url);

        // When the put method is called
        collections.put(request, response);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void TestDelete_ReturnBadRequest_URLSegments() throws Exception {

        shouldAuthorise(request, true);

        // Given a PUT request with a URL that contains less than the expected number of segments
        String url = "/collections/123/instances";

        when(request.getPathInfo()).thenReturn(url);

        // When the delete method is called
        collections.delete(request, response);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void TestDelete_ReturnBadRequest_NotInstanceEndpoint() throws Exception {

        shouldAuthorise(request, true);

        // Given a PUT request with a URL that contains something other than /collections/{}/instances/{}
        String url = "/collections/123/datasets/345";
        when(request.getPathInfo()).thenReturn(url);

        // When the delete method is called
        collections.delete(request, response);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void TestDelete() throws Exception {

        // Given a DELETE request with a valid URL
        String url = String.format("/collections/%s/instances/%s", collectionID, instanceID);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, true);

        // When the delete method is called
        collections.delete(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).deleteInstanceFromCollection(collectionID, instanceID);
    }

    @Test
    public void TestDelete_Forbidden() throws Exception {

        // Given a PUT request with a valid URL
        String url = String.format("/collections/%s/instances/%s", collectionID, instanceID);

        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, false);

        // When the put method is called
        collections.delete(request, response);

        // a HTTP 403 is set on the response
        verify(response).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    // mock the authorisation for the given request to authorise the request is the authorise param is true.
    private void shouldAuthorise(HttpServletRequest request, boolean authorise) throws ZebedeeException, IOException {

        Session session = mock(Session.class);
        when(mockZebedee.getSession(request)).thenReturn(session);

        PermissionsService permissionsService = mock(PermissionsService.class);
        when(mockZebedee.getPermissions()).thenReturn(permissionsService);

        when(permissionsService.canEdit(session)).thenReturn(authorise);
    }
}
