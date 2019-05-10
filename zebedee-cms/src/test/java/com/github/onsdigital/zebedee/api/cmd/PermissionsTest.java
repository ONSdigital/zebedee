package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.authorisation.AuthorisationService;
import com.github.onsdigital.zebedee.authorisation.DatasetPermissionType;
import com.github.onsdigital.zebedee.authorisation.DatasetPermissions;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.github.onsdigital.zebedee.api.cmd.Permissions.COLLECTION_ID_MISSING;
import static com.github.onsdigital.zebedee.api.cmd.Permissions.COLLECTION_ID_PARAM;
import static com.github.onsdigital.zebedee.api.cmd.Permissions.DATASET_ID_MISSING;
import static com.github.onsdigital.zebedee.api.cmd.Permissions.DATASET_ID_PARAM;
import static com.github.onsdigital.zebedee.api.cmd.Permissions.FLORENCE_AUTH_HEATHER;
import static junit.framework.TestCase.assertTrue;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PermissionsTest {

    @Mock
    protected HttpServletRequest mockRequest;

    @Mock
    protected HttpServletResponse mockResponse;

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private HttpResponseWriter httpResponseWriter;

    private DatasetPermissions datasetPermissions;

    @Mock
    private Session session;

    private Permissions api;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * CMD feature is disabled.
     * Expected response: Not found.
     */
    @Test
    public void testCMDFeatureDisabled() throws Exception {
        api = new Permissions(false, authorisationService, httpResponseWriter);

        api.handle(mockRequest, mockResponse);

        verify(httpResponseWriter, times(1)).writeJSONResponse(mockResponse, null, SC_NOT_FOUND);
        verifyNoMoreInteractions(mockRequest, mockResponse, authorisationService);
    }

    /**
     * Request does not contain a Florence auth header or a service auth token.
     * Expected response: Bad Request
     */
    @Test
    public void testGetPermissionNoAuthHeaderOrServiceToken() throws IOException {
        api = new Permissions(true, authorisationService, httpResponseWriter);

        api.handle(mockRequest, mockResponse);

        verify(httpResponseWriter, times(1)).writeJSONResponse(eq(mockResponse), isNull(), eq(400));
    }

    /**
     * Request contains Florence auth header but no dataset_id param.
     * Expected response: Bad request.
     */
    @Test
    public void testUserRequestNoDatasetID() throws Exception {
        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER))
                .thenReturn("666");

        api = new Permissions(true, authorisationService, httpResponseWriter);

        api.handle(mockRequest, mockResponse);

        verify(mockRequest, times(1)).getHeader(FLORENCE_AUTH_HEATHER);
        verify(mockRequest, times(1)).getParameter(DATASET_ID_PARAM);
        verify(httpResponseWriter, times(1)).writeJSONResponse(mockResponse, DATASET_ID_MISSING, SC_BAD_REQUEST);
        verifyNoMoreInteractions(mockResponse, authorisationService, httpResponseWriter);
    }

    /**
     * Request contains Florence auth header, dataset_id param but no collection_id param.
     * Expected response: Bad request.
     */
    @Test
    public void testUserRequestNoCollectionID() throws Exception {
        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER))
                .thenReturn("666");

        when(mockRequest.getParameter(DATASET_ID_PARAM))
                .thenReturn("666");

        api = new Permissions(true, authorisationService, httpResponseWriter);

        api.handle(mockRequest, mockResponse);

        verify(mockRequest, times(1)).getHeader(FLORENCE_AUTH_HEATHER);
        verify(mockRequest, times(1)).getParameter(DATASET_ID_PARAM);
        verify(httpResponseWriter, times(1)).writeJSONResponse(mockResponse, COLLECTION_ID_MISSING, SC_BAD_REQUEST);
        verifyNoMoreInteractions(mockResponse, authorisationService, httpResponseWriter);
    }

    /**
     * Valid request for user dataset permissions. HttpResponseWriter throws an exception.
     * Expected response: internal server error.
     */
    @Test
    public void testGetUserPermissionsHttpResponseWriterException() throws IOException {
        datasetPermissions = new DatasetPermissions(DatasetPermissionType.READ);

        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER))
                .thenReturn("666");

        when(mockRequest.getParameter(DATASET_ID_PARAM))
                .thenReturn("777");

        when(mockRequest.getParameter(COLLECTION_ID_PARAM))
                .thenReturn("888");

        when(authorisationService.getUserPermissions("666", "777", "888"))
                .thenReturn(datasetPermissions);

        ArgumentCaptor<DatasetPermissions> permissionsCaptor = ArgumentCaptor.forClass(DatasetPermissions.class);

        doThrow(new IOException("bang")).when(httpResponseWriter)
                .writeJSONResponse(eq(mockResponse), permissionsCaptor.capture(), eq(200));

        api = new Permissions(true, authorisationService, httpResponseWriter);

        api.handle(mockRequest, mockResponse);

        verify(authorisationService, times(1)).getUserPermissions("666", "777", "888");
        verify(httpResponseWriter, times(1)).writeJSONResponse(eq(mockResponse), permissionsCaptor.capture(), eq(200));
        verify(httpResponseWriter, times(1)).writeJSONResponse(eq(mockResponse), isNull(), eq(500));
    }

    /**
     * Valid request for user dataset permissions.
     * Expected response: status OK, Body - the user's permissions entity.
     */
    @Test
    public void testGetUserPermissionsSuccess() throws IOException {
        datasetPermissions = new DatasetPermissions(DatasetPermissionType.READ);

        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER))
                .thenReturn("666");

        when(mockRequest.getParameter(DATASET_ID_PARAM))
                .thenReturn("777");

        when(mockRequest.getParameter(COLLECTION_ID_PARAM))
                .thenReturn("888");

        when(authorisationService.getUserPermissions("666", "777", "888"))
                .thenReturn(datasetPermissions);

        ArgumentCaptor<DatasetPermissions> permissionsCaptor = ArgumentCaptor.forClass(DatasetPermissions.class);

        api = new Permissions(true, authorisationService, httpResponseWriter);

        api.handle(mockRequest, mockResponse);

        verify(authorisationService, times(1)).getUserPermissions("666", "777", "888");
        verify(httpResponseWriter, times(1)).writeJSONResponse(eq(mockResponse), permissionsCaptor.capture(), eq(200));

        DatasetPermissions actual = permissionsCaptor.getValue();
        assertThat(actual.getPermissions().size(), equalTo(1));
        assertTrue("expected READ permission but not found", actual.getPermissions().contains(DatasetPermissionType.READ));
    }

}
