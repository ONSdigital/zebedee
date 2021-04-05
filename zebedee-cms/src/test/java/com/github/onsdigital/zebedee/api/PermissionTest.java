package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PermissionTest extends ZebedeeAPIBaseTestCase {

    @Mock
    private Sessions sessionsService;

    @Mock
    private PermissionsService permissionsService;

    private PermissionDefinition permission;

    private Permission endpoint;

    @Override
    protected void customSetUp() throws Exception {
        when(sessionsService.get(mockRequest))
                .thenReturn(session);

        permission = new PermissionDefinition();
        permission.setEmail(TEST_EMAIL);

        endpoint = new Permission(sessionsService, permissionsService);
    }

    @Override
    protected Object getAPIName() {
        return "Permission";
    }

    @Test
    public void testGrant_getSessionError_shouldThrowException() throws Exception {
        when(sessionsService.get(mockRequest))
                .thenThrow(IOException.class);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        assertThat(ex.getMessage(), equalTo("error getting user session"));
        verify(sessionsService, times(1)).get(mockRequest);
    }

    @Test
    public void testGrant_getSessionReturnsNull_shouldThrowException() throws Exception {
        when(sessionsService.get(mockRequest))
                .thenReturn(null);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        assertThat(ex.getMessage(), equalTo("error expected user session but was null"));
        verify(sessionsService, times(1)).get(mockRequest);
    }

    @Test
    public void testGrant_addAdminError_shouldThrowException() throws Exception {
        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .addAdministrator(TEST_EMAIL, session);

        permission.isAdmin(true);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(TEST_EMAIL, session);
    }

    @Test
    public void testGrant_addAdminSuccess_shouldAssignAdminPermissions() throws Exception {
        permission.isAdmin(true);

        String actual = endpoint.grantPermission(mockRequest, mockResponse, permission);
        String expected = "Permissions updated for " + TEST_EMAIL;

        assertThat(actual, equalTo(expected));
        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(TEST_EMAIL, session);
        verify(permissionsService, times(1)).addEditor(TEST_EMAIL, session);
    }

    @Test
    public void testGrant_removeAdminError_shouldAssignAdminPermissions() throws Exception {
        permission.isAdmin(false);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .removeAdministrator(TEST_EMAIL, session);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).removeAdministrator(TEST_EMAIL, session);
    }

    @Test
    public void testGrant_addEditorError_shouldAssignEditorPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(true);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .addEditor(TEST_EMAIL, session);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addEditor(TEST_EMAIL, session);
        verify(permissionsService, times(1)).removeAdministrator(TEST_EMAIL, session);
    }

    @Test
    public void testGrant_removeEditorError_shouldAssignAdminPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(false);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .removeEditor(TEST_EMAIL, session);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).removeEditor(TEST_EMAIL, session);
        verify(permissionsService, times(1)).removeAdministrator(TEST_EMAIL, session);
    }

    @Test
    public void testGrant_removeEditorAndAdminSuccess_shouldRevokeEditorAndAdminPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(false);

        String actual = endpoint.grantPermission(mockRequest, mockResponse, permission);
        String expected = "Permissions updated for " + TEST_EMAIL;

        assertThat(actual, equalTo(expected));
        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).removeEditor(TEST_EMAIL, session);
        verify(permissionsService, times(1)).removeAdministrator(TEST_EMAIL, session);
    }
}
