package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.GetPermissionsRequest;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
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
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.datasetIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionNotProvidedException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class UserDatasetPermissionsTest {

    @Mock
    HttpServletRequest req;

    @Mock
    HttpServletResponse resp;

    @Mock
    Session session;

    @Mock
    Sessions sessions;

    @Mock
    HttpResponseWriter httpResponseWriter;

    @Mock
    CMDPermissionsService cmdPermissionsService;

    GetPermissionsRequest getPermissionsRequest;

    UserDatasetPermissions api;

    CRUD fullPermissions;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        fullPermissions = new CRUD().permit(CREATE, READ, UPDATE, DELETE);

        getPermissionsRequest = new GetPermissionsRequest(session, "222", "333", "444");

        api = new UserDatasetPermissions(cmdPermissionsService, httpResponseWriter, sessions);

        when(sessions.get()).thenReturn(session);
        when(session.getId()).thenReturn("111");
    }

    @Test
    public void testGetDatasetPermissions_success() throws Exception {
        when(req.getParameter("dataset_id"))
                .thenReturn("dataset_id");

        when(req.getParameter("collection_id"))
                .thenReturn("collection_id");

        when(req.getHeader("X-Florence-Token"))
                .thenReturn("X-Florence-Token");

        when(cmdPermissionsService.getUserDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenReturn(fullPermissions);

        api.handle(req, resp);

        verify(cmdPermissionsService, times(1))
                .getUserDatasetPermissions(any(GetPermissionsRequest.class));
        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, fullPermissions, 200);
    }

    @Test
    public void testGetDatasetPermissions_CMDPermissionsServiceException() throws Exception {
        when(req.getParameter("dataset_id"))
                .thenReturn("dataset_id");

        when(req.getParameter("collection_id"))
                .thenReturn("collection_id");

        when(cmdPermissionsService.getUserDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenThrow(new PermissionsException("boom", 500));

        api.handle(req, resp);

        verify(cmdPermissionsService, times(1))
                .getUserDatasetPermissions(any(GetPermissionsRequest.class));

        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error("boom"), 500);
    }


    @Test
    public void testGetDatasetPermissions_BadRequest() throws Exception {
        when(sessions.get()).thenReturn(null);

        api.handle(req, resp);

        verifyNoInteractions(cmdPermissionsService);

        PermissionsException expected = sessionNotProvidedException();

        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error(expected.getMessage()), expected.statusCode);
    }

    @Test
    public void testGetDatasetPermissions_writeSuccessResponseFailure() throws Exception {
        when(req.getParameter("dataset_id"))
                .thenReturn("dataset_id");

        when(req.getParameter("collection_id"))
                .thenReturn("collection_id");

        when(req.getHeader("X-Florence-Token"))
                .thenReturn("X-Florence-Token");

        when(cmdPermissionsService.getUserDatasetPermissions(any()))
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
                .getUserDatasetPermissions(any());

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

        when(req.getParameter("collection_id"))
                .thenReturn("collection_id");

        when(req.getHeader("X-Florence-Token"))
                .thenReturn("X-Florence-Token");

        when(cmdPermissionsService.getUserDatasetPermissions(any()))
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
                .getUserDatasetPermissions(any());

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
    public void testGetDatasePermissions_sessionNull() throws Exception {
        when(sessions.get()).thenReturn(null);
        when(cmdPermissionsService.getUserDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenReturn(fullPermissions);

        api.handle(req, resp);

        verifyNoInteractions(cmdPermissionsService);

        PermissionsException expected = sessionNotProvidedException();
        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error(expected.getMessage()), expected.statusCode);
    }

    @Test
    public void testGetDatasePermissions_datasetIDNull() throws Exception {
        when(req.getHeader("X-Florence-Token"))
                .thenReturn("X-Florence-Token");

        when(cmdPermissionsService.getUserDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenReturn(fullPermissions);

        api.handle(req, resp);

        verifyNoInteractions(cmdPermissionsService);

        PermissionsException expected = datasetIDNotProvidedException();
        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error(expected.getMessage()), expected.statusCode);
    }

    @Test
    public void testGetDatasePermissions_collectionIDNull() throws Exception {
        when(req.getHeader("X-Florence-Token"))
                .thenReturn("X-Florence-Token");

        when(req.getParameter("dataset_id"))
                .thenReturn("dataset_id");

        when(cmdPermissionsService.getUserDatasetPermissions(any(GetPermissionsRequest.class)))
                .thenReturn(fullPermissions);

        api.handle(req, resp);

        verifyNoInteractions(cmdPermissionsService);

        PermissionsException expected = collectionIDNotProvidedException();
        verify(httpResponseWriter, times(1))
                .writeJSONResponse(resp, new Error(expected.getMessage()), expected.statusCode);
    }

}
