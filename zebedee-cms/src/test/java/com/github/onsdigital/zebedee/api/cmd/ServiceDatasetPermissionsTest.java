package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.GetPermissionsRequest;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.datasetIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceTokenNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionIDNotProvidedException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServiceDatasetPermissionsTest {

    @Mock
    HttpServletRequest req;

    @Mock
    HttpServletResponse resp;

    @Mock
    HttpResponseWriter httpResponseWriter;

    @Mock
    CMDPermissionsService cmdPermissionsService;

    GetPermissionsRequest getPermissionsRequest;

    ServiceDatasetPermissions api;

    CRUD fullPermissions;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fullPermissions = new CRUD().permit(CREATE, READ, UPDATE, DELETE);

        getPermissionsRequest = new GetPermissionsRequest("111", "222", "333", "444");

        api = new ServiceDatasetPermissions(true, cmdPermissionsService, httpResponseWriter);
    }

    @Test
    public void testGetDatasetPermissions_featureDisabled() throws Exception {
        api = new ServiceDatasetPermissions(false, cmdPermissionsService, httpResponseWriter);

        api.handle(req, resp);

        verifyZeroInteractions(cmdPermissionsService);
        verify(httpResponseWriter, times(1)).writeJSONResponse(resp, null, 404);
    }

    @Test
    public void testGetDatasetPermissions_success() throws Exception {
        when(req.getParameter("dataset_id"))
                .thenReturn("dataset_id");

        when(req.getHeader("Authorization"))
                .thenReturn("Bearer Authorization");

        when(cmdPermissionsService.getServiceDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenReturn(fullPermissions);

        api.handle(req, resp);

        verify(cmdPermissionsService, times(1))
                .getServiceDatasetPermissions(any(GetPermissionsRequest.class));

        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, fullPermissions, 200);
    }

    @Test
    public void testGetDatasetPermissions_cmdPermissionsServiceException() throws Exception {
        when(req.getParameter("dataset_id"))
                .thenReturn("dataset_id");

        when(req.getHeader("Authorization"))
                .thenReturn("Bearer Authorization");

        when(cmdPermissionsService.getServiceDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenThrow(new PermissionsException("boom", 500));

        api.handle(req, resp);

        verify(cmdPermissionsService, times(1))
                .getServiceDatasetPermissions(any(GetPermissionsRequest.class));

        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error("boom"), 500);
    }

    @Test
    public void testGetDatasetPermissions_BadRequest() throws Exception {
        api.handle(req, resp);

        verifyZeroInteractions(cmdPermissionsService);

        PermissionsException expected = serviceTokenNotProvidedException();

        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error(expected.getMessage()), expected.statusCode);
    }

    @Test
    public void testGetDatasetPermissions_writeSuccessResponseFailure() throws Exception {
        when(req.getParameter("dataset_id"))
                .thenReturn("dataset_id");

        when(req.getHeader("Authorization"))
                .thenReturn("Bearer Authorization");

        when(cmdPermissionsService.getServiceDatasetPermissions(any()))
                .thenReturn(fullPermissions);

        ArgumentCaptor<HttpServletResponse> responseCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);
        ArgumentCaptor<Object> entityCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);

        doThrow(new IOException("boom"))
                .doNothing()
                .when(httpResponseWriter).writeJSONResponse(
                responseCaptor.capture(),
                entityCaptor.capture(),
                statusCaptor.capture()
        );

        api.handle(req, resp);

        verify(cmdPermissionsService, times(1))
                .getServiceDatasetPermissions(any());

        List<HttpServletResponse> responseArgs = responseCaptor.getAllValues();
        assertThat(responseArgs.size(), equalTo(2));

        List<Object> entityArgs = entityCaptor.getAllValues();
        assertThat(entityArgs.size(), equalTo(2));
        assertThat(entityArgs.get(0), equalTo(fullPermissions));
        assertThat(entityArgs.get(1), CoreMatchers.is(nullValue()));

        List<Integer> statusArgs = statusCaptor.getAllValues();
        assertThat(statusArgs.size(), equalTo(2));
        assertThat(statusArgs.get(0), equalTo(200));
        assertThat(statusArgs.get(1), equalTo(500));
    }

    @Test
    public void testGetDatasetPermissions_writeReponseCompleteFailure() throws Exception {
        when(req.getParameter("dataset_id"))
                .thenReturn("dataset_id");

        when(req.getHeader("Authorization"))
                .thenReturn("Bearer Authorization");

        when(cmdPermissionsService.getServiceDatasetPermissions(any()))
                .thenReturn(fullPermissions);

        ArgumentCaptor<HttpServletResponse> responseCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);
        ArgumentCaptor<Object> entityCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);

        doThrow(new IOException("boom"))
                .when(httpResponseWriter).writeJSONResponse(
                responseCaptor.capture(),
                entityCaptor.capture(),
                statusCaptor.capture()
        );

        api.handle(req, resp);

        verify(cmdPermissionsService, times(1))
                .getServiceDatasetPermissions(any());

        verify(resp, times(1))
                .setStatus(500);

        List<HttpServletResponse> responseArgs = responseCaptor.getAllValues();
        assertThat(responseArgs.size(), equalTo(2));

        List<Object> entityArgs = entityCaptor.getAllValues();
        assertThat(entityArgs.size(), equalTo(2));
        assertThat(entityArgs.get(0), equalTo(fullPermissions));
        assertThat(entityArgs.get(1), CoreMatchers.is(nullValue()));

        List<Integer> statusArgs = statusCaptor.getAllValues();
        assertThat(statusArgs.size(), equalTo(2));
        assertThat(statusArgs.get(0), equalTo(200));
        assertThat(statusArgs.get(1), equalTo(500));
    }

    @Test
    public void testGetDatasePermissions_serviceAccountIDNull() throws Exception {
        when(cmdPermissionsService.getUserDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenReturn(fullPermissions);

        api.handle(req, resp);

        verifyZeroInteractions(cmdPermissionsService);

        PermissionsException expected = serviceTokenNotProvidedException();
        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error(expected.getMessage()), expected.statusCode);
    }

    @Test
    public void testGetDatasePermissions_datasetIDNull() throws Exception {
        when(req.getHeader("Authorization"))
                .thenReturn("Bearer Authorization");

        when(cmdPermissionsService.getUserDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenReturn(fullPermissions);

        api.handle(req, resp);

        verifyZeroInteractions(cmdPermissionsService);

        PermissionsException expected = datasetIDNotProvidedException();
        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error(expected.getMessage()), expected.statusCode);
    }
}
