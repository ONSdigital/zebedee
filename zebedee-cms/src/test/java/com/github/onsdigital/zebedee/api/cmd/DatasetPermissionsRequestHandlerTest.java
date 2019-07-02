package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.api.cmd.PermissionsRequestHandler.COLLECTION_ID_PARAM;
import static com.github.onsdigital.zebedee.api.cmd.PermissionsRequestHandler.DATASET_ID_PARAM;
import static com.github.onsdigital.zebedee.api.cmd.PermissionsRequestHandler.FLORENCE_AUTH_HEATHER;
import static com.github.onsdigital.zebedee.api.cmd.PermissionsRequestHandler.SERVICE_AUTH_HEADER;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DatasetPermissionsRequestHandlerTest {

    static final String SESSION_ID = "666";
    static final String DATASET_ID = "667";
    static final String COLLECTION_ID = "668";
    static final String SERVICE_TOKEN = "669";

    @Mock
    protected HttpServletRequest mockRequest;

    @Mock
    protected HttpServletResponse mockResponse;

    @Mock
    private HttpResponseWriter httpResponseWriter;

    private CRUD datasetPermissions;

    @Mock
    private Session session;

    private DatasetPermissionsRequestHandler handler;

    @Mock
    private PermissionsService permissionsService;

    private CRUD fullPermissions;
    private CRUD readOnly;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new DatasetPermissionsRequestHandler(permissionsService);
        fullPermissions = new CRUD().permit(CREATE, READ, UPDATE, DELETE);
        readOnly = new CRUD().permit(READ);
    }

    @Test(expected = PermissionsException.class)
    public void getPermissions_sessionIDAndServiceTokenNull() throws Exception {
        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER)).thenReturn(null);
        when(mockRequest.getHeader(SERVICE_AUTH_HEADER)).thenReturn(null);

        try {
            handler.get(mockRequest, mockResponse);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(permissionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void getPermissions_getUserDatasetPermissionsExeception() throws Exception {
        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER)).thenReturn(SESSION_ID);
        when(mockRequest.getHeader(SERVICE_AUTH_HEADER)).thenReturn(null);
        when(mockRequest.getParameter(DATASET_ID_PARAM)).thenReturn(DATASET_ID);
        when(mockRequest.getParameter(COLLECTION_ID_PARAM)).thenReturn(COLLECTION_ID);

        when(permissionsService.getUserDatasetPermissions(SESSION_ID, DATASET_ID, COLLECTION_ID))
                .thenThrow(new PermissionsException("", HttpStatus.SC_INTERNAL_SERVER_ERROR));

        try {
            handler.get(mockRequest, mockResponse);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(permissionsService, times(1)).getUserDatasetPermissions(SESSION_ID, DATASET_ID, COLLECTION_ID);
            verifyZeroInteractions(permissionsService);
            throw ex;
        }
    }

    @Test
    public void getPermissions_getUserDatasetPermissionsSuccess() throws Exception {
        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER)).thenReturn(SESSION_ID);
        when(mockRequest.getHeader(SERVICE_AUTH_HEADER)).thenReturn(null);
        when(mockRequest.getParameter(DATASET_ID_PARAM)).thenReturn(DATASET_ID);
        when(mockRequest.getParameter(COLLECTION_ID_PARAM)).thenReturn(COLLECTION_ID);

        when(permissionsService.getUserDatasetPermissions(SESSION_ID, DATASET_ID, COLLECTION_ID))
                .thenReturn(readOnly);

        CRUD actual = handler.get(mockRequest, mockResponse);

        assertThat(actual, equalTo(readOnly));
        verify(permissionsService, times(1)).getUserDatasetPermissions(SESSION_ID, DATASET_ID, COLLECTION_ID);
        verifyZeroInteractions(permissionsService);
    }

    @Test(expected = PermissionsException.class)
    public void getPermissions_getServiceDatasetPermissionsExeception() throws Exception {
        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER)).thenReturn(null);
        when(mockRequest.getHeader(SERVICE_AUTH_HEADER)).thenReturn(SERVICE_TOKEN);
        when(mockRequest.getParameter(DATASET_ID_PARAM)).thenReturn(DATASET_ID);
        when(mockRequest.getParameter(COLLECTION_ID_PARAM)).thenReturn(COLLECTION_ID);

        when(permissionsService.getServiceDatasetPermissions(SERVICE_TOKEN, DATASET_ID))
                .thenThrow(new PermissionsException("", HttpStatus.SC_INTERNAL_SERVER_ERROR));

        try {
            handler.get(mockRequest, mockResponse);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(permissionsService, times(1)).getServiceDatasetPermissions(SERVICE_TOKEN, DATASET_ID);
            verifyZeroInteractions(permissionsService);
            throw ex;
        }
    }

    @Test
    public void getPermissions_getServiceDatasetPermissionsSuccess() throws Exception {
        when(mockRequest.getHeader(FLORENCE_AUTH_HEATHER)).thenReturn(null);
        when(mockRequest.getHeader(SERVICE_AUTH_HEADER)).thenReturn(SERVICE_TOKEN);
        when(mockRequest.getParameter(DATASET_ID_PARAM)).thenReturn(DATASET_ID);
        when(mockRequest.getParameter(COLLECTION_ID_PARAM)).thenReturn(COLLECTION_ID);

        when(permissionsService.getServiceDatasetPermissions(SERVICE_TOKEN, DATASET_ID))
                .thenReturn(fullPermissions);

        CRUD actual = handler.get(mockRequest, mockResponse);

        assertThat(actual, equalTo(fullPermissions));
        verify(permissionsService, times(1)).getServiceDatasetPermissions(SERVICE_TOKEN, DATASET_ID);
        verifyZeroInteractions(permissionsService);
    }

    @Test
    public void testParseServiceToken_withPrefix() {
        assertThat(handler.parseServiceToken("Bearer 666"), equalTo("666"));
    }

    @Test
    public void testParseServiceToken_withoutPrefix() {
        assertThat(handler.parseServiceToken("666"), equalTo("666"));
    }
}
