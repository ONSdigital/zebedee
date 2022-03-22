package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Test;
import org.mockito.Mock;

import javax.crypto.SecretKey;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PermissionTest extends ZebedeeAPIBaseTestCase {

    private static final String USER_EMAIL = "abc@ons.gov.uk";

    @Mock
    private Sessions sessionsService;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private UsersService usersService;

    @Mock
    private Collections collections;

    @Mock
    private Collection collectionMock;

    @Mock
    private CollectionDescription collectionDescription;

    @Mock
    private CollectionKeyring collectionKeyring;

    @Mock
    private User srcUser, targetUser;

    @Mock
    private SecretKey key;

    private PermissionDefinition permission;

    private Permission endpoint;

    @Override
    protected void customSetUp() throws Exception {
        when(sessionsService.get())
                .thenReturn(mockSession);

        Collections.CollectionList collectionList = new Collections.CollectionList();
        collectionList.add(collectionMock);

        when(collections.list())
                .thenReturn(collectionList);

        when(collectionKeyring.get(any(), any()))
                .thenReturn(key);

        when(collectionMock.getDescription())
                .thenReturn(collectionDescription);

        permission = new PermissionDefinition();
        permission.setEmail(USER_EMAIL);

        endpoint = new Permission(sessionsService, permissionsService, usersService, collections, collectionKeyring);
    }

    @Override
    protected Object getAPIName() {
        return "Permission";
    }

    @Test
    public void testGrant_getSessionReturnsNull_shouldThrowException() throws Exception {
        when(sessionsService.get())
                .thenReturn(null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        assertThat(ex.getMessage(), equalTo("error expected user session but was null"));
        verify(sessionsService, times(1)).get();
    }

    @Test
    public void testGrant_addAdminError_shouldThrowException() throws Exception {
        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .addAdministrator(USER_EMAIL, mockSession);

        permission.isAdmin(true);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get();
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, mockSession);
    }

    @Test
    public void testGrant_addAdminAssignSuccess_shouldAssignPermissionsAndKeys() throws Exception {
        permission.isAdmin(true);

        endpoint.grantPermission(mockRequest, mockResponse, permission);

        verify(sessionsService, times(1)).get();
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, mockSession);
    }

    @Test
    public void testGrant_removeAdminError_shouldRemoveAdminPermissions() throws Exception {
        permission.isAdmin(false);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .removeAdministrator(USER_EMAIL, mockSession);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get();
        verify(permissionsService, times(1)).removeAdministrator(USER_EMAIL, mockSession);
    }

    @Test
    public void testGrant_addEditorError_shouldAssignEditorPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(true);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .addEditor(USER_EMAIL, mockSession);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get();
        verify(permissionsService, times(1)).addEditor(USER_EMAIL, mockSession);
        verify(permissionsService, times(1)).removeAdministrator(USER_EMAIL, mockSession);
    }

    @Test
    public void testGrant_removeEditorError_shouldRemoveAdminAndEditorPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(false);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .removeEditor(USER_EMAIL, mockSession);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get();
        verify(permissionsService, times(1)).removeEditor(USER_EMAIL, mockSession);
        verify(permissionsService, times(1)).removeAdministrator(USER_EMAIL, mockSession);
    }

    @Test
    public void testGrant_removeEditorAndAdminSuccess_shouldRevokeEditorAndAdminPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(false);

        String actual = endpoint.grantPermission(mockRequest, mockResponse, permission);
        String expected = "Permissions updated for " + USER_EMAIL;

        assertThat(actual, equalTo(expected));
        verify(sessionsService, times(1)).get();
        verify(permissionsService, times(1)).removeEditor(USER_EMAIL, mockSession);
        verify(permissionsService, times(1)).removeAdministrator(USER_EMAIL, mockSession);
    }
}
