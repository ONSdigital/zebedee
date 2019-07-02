package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DatasetPermissionsTest {

    @Mock
    HttpServletRequest req;

    @Mock
    HttpServletResponse resp;

    @Mock
    PermissionsRequestHandler permissionsRequestHandler;

    @Mock
    HttpResponseWriter httpResponseWriter;

    DatasetPermissions api;

    CRUD fullPermissions;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fullPermissions = new CRUD().permit(CREATE, READ, UPDATE, DELETE);

        api = new DatasetPermissions(true, permissionsRequestHandler, httpResponseWriter);
    }

    @Test
    public void testGetDatasetPermissions_success() throws Exception {
        when(permissionsRequestHandler.get(req, resp)).thenReturn(fullPermissions);

        api.handle(req, resp);

        verify(permissionsRequestHandler, times(1)).get(req, resp);
        verify(httpResponseWriter, times(1)).writeJSONResponse(resp, fullPermissions, 200);
    }

    @Test
    public void testGetDatasetPermissions_permissionRequestHandlerException() throws Exception {
        when(permissionsRequestHandler.get(req, resp)).thenThrow(new PermissionsException("boom", 500));

        api.handle(req, resp);

        verify(permissionsRequestHandler, times(1)).get(req, resp);
        verify(httpResponseWriter, times(1)).writeJSONResponse(resp, new Error("boom"), 500);
    }

    @Test
    public void testGetDatasetPermissions_featureDisabled() throws Exception {
        api = new DatasetPermissions(false, permissionsRequestHandler, httpResponseWriter);

        api.handle(req, resp);

        verifyZeroInteractions(permissionsRequestHandler);
        verify(httpResponseWriter, times(1)).writeJSONResponse(resp, null, 404);
    }

    @Test
    public void testGetDatasetPermissions_writeSuccessResponseFailure() throws Exception {
        when(permissionsRequestHandler.get(req, resp)).thenReturn(fullPermissions);

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

        verify(permissionsRequestHandler, times(1)).get(req, resp);

        List<HttpServletResponse> responseArgs = responseCaptor.getAllValues();
        assertThat(responseArgs.size(), equalTo(2));

        List<Object> entityArgs = entityCaptor.getAllValues();
        assertThat(entityArgs.size(), equalTo(2));
        assertThat(entityArgs.get(0), equalTo(fullPermissions));
        assertThat(entityArgs.get(1), is(nullValue()));

        List<Integer> statusArgs = statusCaptor.getAllValues();
        assertThat(statusArgs.size(), equalTo(2));
        assertThat(statusArgs.get(0), equalTo(200));
        assertThat(statusArgs.get(1), equalTo(500));
    }

    @Test
    public void testGetDatasetPermissions_writeReponseCompleteFailure() throws Exception {
        when(permissionsRequestHandler.get(req, resp)).thenReturn(fullPermissions);

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

        verify(permissionsRequestHandler, times(1)).get(req, resp);
        verify(resp, times(1)).setStatus(500);

        List<HttpServletResponse> responseArgs = responseCaptor.getAllValues();
        assertThat(responseArgs.size(), equalTo(2));

        List<Object> entityArgs = entityCaptor.getAllValues();
        assertThat(entityArgs.size(), equalTo(2));
        assertThat(entityArgs.get(0), equalTo(fullPermissions));
        assertThat(entityArgs.get(1), is(nullValue()));

        List<Integer> statusArgs = statusCaptor.getAllValues();
        assertThat(statusArgs.size(), equalTo(2));
        assertThat(statusArgs.get(0), equalTo(200));
        assertThat(statusArgs.get(1), equalTo(500));
    }
}
