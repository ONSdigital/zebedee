package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.GetPermissionsRequest;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionNotProvidedException;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UserInstancePermissionsTest {

    static final String FLORENCE_HEADER = "X-Florence-Token";
    static final String SESSION_ID = "42";

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    Session session;

    @Mock
    Sessions sessions;

    @Mock
    CMDPermissionsService permissionsService;

    @Mock
    HttpResponseWriter httpResponseWriter;

    UserInstancePermissions api;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        api = new UserInstancePermissions(permissionsService, httpResponseWriter, sessions);

        when(sessions.get()).thenReturn(session);
        when(session.getId()).thenReturn(SESSION_ID);
    }

    @Test
    public void givenAValidRequest() throws Exception {
        GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(session, null, null, null);

        CRUD expected = new CRUD().permit(CREATE, READ, UPDATE, DELETE);

        when(permissionsService.getUserInstancePermissions(getPermissionsRequest))
                .thenReturn(expected);

        api.handle(request, response);

        verify(permissionsService, times(1)).getUserInstancePermissions(getPermissionsRequest);
        verify(httpResponseWriter, times(1)).writeJSONResponse(response, expected, SC_OK);
    }

    @Test
    public void givenRequestDoesNotContainASession() throws Exception {
        when(sessions.get()).thenReturn(null);

        api.handle(request, response);

        Error expected = new Error(sessionNotProvidedException().getMessage());

        verify(httpResponseWriter, times(1)).writeJSONResponse(response, expected, SC_BAD_REQUEST);
        verifyZeroInteractions(permissionsService);
    }

    @Test
    public void givenPermissionsServiceThrowsPermissionsException() throws Exception {
        GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(session, null, null, null);

        when(permissionsService.getUserInstancePermissions(getPermissionsRequest))
                .thenThrow(internalServerErrorException());

        api.handle(request, response);

        Error expected = new Error(internalServerErrorException().getMessage());

        verify(permissionsService, times(1)).getUserInstancePermissions(getPermissionsRequest);
        verify(httpResponseWriter, times(1)).writeJSONResponse(response, expected, SC_INTERNAL_SERVER_ERROR);
    }
}
