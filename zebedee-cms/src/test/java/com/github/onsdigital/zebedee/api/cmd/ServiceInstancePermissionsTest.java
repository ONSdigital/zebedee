package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.GetPermissionsRequest;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.api.cmd.PermissionsAPIBase.SERVICE_AUTH_HEADER;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceTokenNotProvidedException;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServiceInstancePermissionsTest {

    static final String SERVICE_AUTH_TOKEN = "666";

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    CMDPermissionsService cmdPermissionsService;

    @Mock
    HttpResponseWriter httpResponseWriter;

    ServiceInstancePermissions api;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        api = new ServiceInstancePermissions(true, cmdPermissionsService, httpResponseWriter);
    }

    @Test
    public void givenFeatureIsDisabled() throws Exception {
        api = new ServiceInstancePermissions(false, cmdPermissionsService, httpResponseWriter);

        api.handle(request, response);

        verify(httpResponseWriter, times(1)).writeJSONResponse(response, null, HttpStatus.SC_NOT_FOUND);
        verifyNoMoreInteractions(cmdPermissionsService);
    }

    @Test
    public void givenARequestWithNoServiceTokenHeader() throws Exception {
        when(request.getHeader(SERVICE_AUTH_HEADER)).thenReturn(null);

        api.handle(request, response);

        Error expected = new Error(serviceTokenNotProvidedException().getMessage());

        verify(httpResponseWriter, times(1)).writeJSONResponse(response, expected, SC_BAD_REQUEST);
        verifyZeroInteractions(cmdPermissionsService);
    }

    @Test
    public void givenCMDPermissionsServiceThrowsPermissionsException() throws Exception {
        GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(null, SERVICE_AUTH_TOKEN, null, null);

        when(request.getHeader(SERVICE_AUTH_HEADER)).thenReturn(SERVICE_AUTH_TOKEN);

        when(cmdPermissionsService.getServiceInstancePermissions(getPermissionsRequest))
                .thenThrow(internalServerErrorException());

        api.handle(request, response);

        Error expected = new Error(internalServerErrorException().getMessage());

        verify(cmdPermissionsService, times(1)).getServiceInstancePermissions(getPermissionsRequest);
        verify(httpResponseWriter, times(1)).writeJSONResponse(response, expected, SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void getAValidRequest() throws Exception {
        GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(null, SERVICE_AUTH_TOKEN, null, null);

        CRUD expected = new CRUD().permit(CREATE, READ, UPDATE, DELETE);

        when(request.getHeader(SERVICE_AUTH_HEADER)).thenReturn(SERVICE_AUTH_TOKEN);

        when(cmdPermissionsService.getServiceInstancePermissions(getPermissionsRequest)).thenReturn(expected);

        api.handle(request, response);

        verify(cmdPermissionsService, times(1)).getServiceInstancePermissions(getPermissionsRequest);
        verify(httpResponseWriter, times(1)).writeJSONResponse(response, expected, SC_OK);
    }
}
