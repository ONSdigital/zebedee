package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.DatasetService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.mockito.Matchers;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CollectionsTest {

    private ZebedeeCmsService mockZebedee = mock(ZebedeeCmsService.class);
    private DatasetService mockDatasetService = mock(DatasetService.class);
    private ServletInputStream mockServletInputStream = mock(ServletInputStream.class);

    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);

    private Collections collections = new Collections(mockZebedee, mockDatasetService);
    private String collectionID = "123";
    private String datasetID = "345";
    private String edition = "2014";
    private String version = "1";


    @Test
    public void TestPutDataset_EmptyJSON() throws Exception {

        // Given a PUT request with a bad json input
        String url = String.format("/collections/%s/datasets/%s", collectionID, datasetID);
        when(request.getPathInfo()).thenReturn(url);

        String json = "";
        mockRequestBody(json);

        shouldAuthorise(request, true);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).updateDatasetInCollection(collectionID, datasetID, null);
    }

    @Test
    public void TestPutDataset() throws Exception {

        // Given a PUT request with a valid URL for a dataset
        String url = String.format("/collections/%s/datasets/%s", collectionID, datasetID);
        when(request.getPathInfo()).thenReturn(url);

        String json = "{ \"state\": \"inProgress\"}";
        mockRequestBody(json);

        shouldAuthorise(request, true);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).updateDatasetInCollection(collectionID, datasetID, new CollectionDataset());
    }

    @Test
    public void TestPutDatasetVersion() throws Exception {

        // Given a PUT request with a valid URL for a dataset
        String url = String.format("/collections/%s/datasets/%s/editions/%s/versions/%s",
                collectionID, datasetID, edition, version);
        when(request.getPathInfo()).thenReturn(url);

        String json = "{ \"state\": \"inProgress\"}";
        mockRequestBody(json);

        shouldAuthorise(request, true);

        // When the put method is called
        collections.put(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).updateDatasetVersionInCollection(
                collectionID, datasetID, edition, version, new CollectionDatasetVersion());
    }

    @Test
    public void TestPut_Forbidden() throws Exception {

        // Given a PUT request with a valid URL
        String url = String.format("/collections/%s/datasets/%s", collectionID, datasetID);
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

        // Given a PUT request with a URL that contains less than the expected 4 segments
        String url = "/collections/123/datasets";
        when(request.getPathInfo()).thenReturn(url);

        // When the put method is called
        collections.put(request, response);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void TestPut_ReturnBadRequest_NotDatasetsEndpoint() throws Exception {

        shouldAuthorise(request, true);

        // Given a PUT request with a URL that contains something other than /collections/{}/datasets/{}
        String url = "/collections/123/wut/345";
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
        String url = "/collections/123/datasets";

        when(request.getPathInfo()).thenReturn(url);

        // When the delete method is called
        collections.delete(request, response);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void TestDelete_ReturnBadRequest_NotDatasetsEndpoint() throws Exception {

        shouldAuthorise(request, true);

        // Given a PUT request with a URL that contains something other than /collections/{}/datasets/{}
        String url = "/collections/123/wut/345";
        when(request.getPathInfo()).thenReturn(url);

        // When the delete method is called
        collections.delete(request, response);

        // Then a HTTP 404 is set on the response.
        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void TestDeleteDataset() throws Exception {

        // Given a DELETE request with a valid URL
        String url = String.format("/collections/%s/datasets/%s", collectionID, datasetID);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, true);

        // When the delete method is called
        collections.delete(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).removeDatasetFromCollection(collectionID, datasetID);
    }

    @Test
    public void TestDeleteDatasetVersion() throws Exception {

        // Given a DELETE request with a valid URL
        String url = String.format("/collections/%s/datasets/%s/editions/%s/versions/%s",
                collectionID, datasetID, edition, version);
        when(request.getPathInfo()).thenReturn(url);

        shouldAuthorise(request, true);

        // When the delete method is called
        collections.delete(request, response);

        // The dataset service is called with the values extracted from the request URL.
        verify(mockDatasetService, times(1)).removeDatasetVersionFromCollection(
                collectionID, datasetID, edition, version);
    }

    @Test
    public void TestDelete_Forbidden() throws Exception {

        // Given a PUT request with a valid URL
        String url = String.format("/collections/%s/datasets/%s", collectionID, datasetID);

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

    private void mockRequestBody(String json) throws IOException {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes());

        // needed this monstrosity to mock the request body input stream.
        when(mockServletInputStream.read(Matchers.any(), anyInt(), anyInt())).thenAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            byte[] output = (byte[]) args[0];
            int offset = (int) args[1];
            int length = (int) args[2];
            return byteArrayInputStream.read(output, offset, length);
        });

        when(request.getInputStream()).thenReturn(mockServletInputStream);
    }
}
