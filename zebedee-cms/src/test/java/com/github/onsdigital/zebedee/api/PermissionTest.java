package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.keyring.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Test;
import org.mockito.Mock;

import javax.crypto.SecretKey;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
    private Keyring keyring;

    @Mock
    private User user1, user2;

    @Mock
    private SecretKey key;

    private PermissionDefinition permission;

    private Permission endpoint;

    @Override
    protected void customSetUp() throws Exception {
        when(sessionsService.get(mockRequest))
                .thenReturn(session);

        when(usersService.getUserByEmail(TEST_EMAIL))
                .thenReturn(user1);

        when(usersService.getUserByEmail(USER_EMAIL))
                .thenReturn(user2);

        Collections.CollectionList collectionList = new Collections.CollectionList();
        collectionList.add(collectionMock);

        when(collections.list())
                .thenReturn(collectionList);

        when(keyring.get(any(), any()))
                .thenReturn(key);

        permission = new PermissionDefinition();
        permission.setEmail(USER_EMAIL);

        endpoint = new Permission(sessionsService, permissionsService, usersService, collections, keyring);
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
                .addAdministrator(USER_EMAIL, session);

        permission.isAdmin(true);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
    }

    @Test
    public void testGrant_addAdminGetUserError_shouldThrowException() throws Exception {
        doThrow(IOException.class)
                .when(usersService)
                .getUserByEmail(any());

        permission.isAdmin(true);

        assertThrows(InternalServerError.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(usersService, times(1)).getUserByEmail(any());
    }

    @Test
    public void testGrant_addAdminUserNull_shouldThrowException() throws Exception {
        when(usersService.getUserByEmail(any()))
                .thenReturn(null);

        permission.isAdmin(true);

        assertThrows(NotFoundException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(usersService, times(1)).getUserByEmail(any());
    }

    @Test
    public void testGrant_collectionsListErr_shouldThrowException() throws Exception {
        when(collections.list())
                .thenThrow(IOException.class);

        permission.isAdmin(true);

        assertThrows(IOException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(usersService, times(1)).getUserByEmail(any());
    }

    @Test
    public void testGrant_collectionsListNull_shouldNotUpdateKeyring() throws Exception {
        when(collections.list())
                .thenReturn(null);

        permission.isAdmin(true);

        endpoint.grantPermission(mockRequest, mockResponse, permission);

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(usersService, times(1)).getUserByEmail(any());
        verify(keyring, never()).add(any(), any(), any());
    }

    @Test
    public void testGrant_collectionsListEmpty_shouldNotUpdateKeyring() throws Exception {
        when(collections.list())
                .thenReturn(new Collections.CollectionList());

        permission.isAdmin(true);

        endpoint.grantPermission(mockRequest, mockResponse, permission);

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(usersService, times(1)).getUserByEmail(any());
        verify(keyring, never()).add(any(), any(), any());
    }

    @Test
    public void testGrant_getKeyError_shouldThrowException() throws Exception {
        when(keyring.get(user1, collectionMock))
                .thenThrow(KeyringException.class);

        permission.isAdmin(true);

        assertThrows(IOException.class, ()
                -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(usersService, times(1)).getUserByEmail(any());
        verify(keyring, times(1)).get(user1, collectionMock);
        verify(keyring, never()).add(any(), any(), any());
    }


    @Test
    public void testGrant_addAdminSuccess_shouldAssignAdminPermissions() throws Exception {
        permission.isAdmin(true);

        String actual = endpoint.grantPermission(mockRequest, mockResponse, permission);
        String expected = "Permissions updated for " + USER_EMAIL;

        assertThat(actual, equalTo(expected));
        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(permissionsService, times(1)).addEditor(USER_EMAIL, session);
        verify(keyring, times(1)).get(user1, collectionMock);
        verify(keyring, times(1)).add(user1, collectionMock, key);
    }

    @Test
    public void testGrant_removeAdminError_shouldRemoveAdminPermissions() throws Exception {
        permission.isAdmin(false);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .removeAdministrator(USER_EMAIL, session);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).removeAdministrator(USER_EMAIL, session);
    }

    @Test
    public void testGrant_addEditorError_shouldAssignEditorPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(true);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .addEditor(USER_EMAIL, session);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addEditor(USER_EMAIL, session);
        verify(permissionsService, times(1)).removeAdministrator(USER_EMAIL, session);
    }

    @Test
    public void testGrant_removeEditorError_shouldAssignAdminPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(false);

        doThrow(UnauthorizedException.class)
                .when(permissionsService)
                .removeEditor(USER_EMAIL, session);

        assertThrows(UnauthorizedException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).removeEditor(USER_EMAIL, session);
        verify(permissionsService, times(1)).removeAdministrator(USER_EMAIL, session);
    }

    @Test
    public void testGrant_removeEditorAndAdminSuccess_shouldRevokeEditorAndAdminPermissions() throws Exception {
        permission.isAdmin(false);
        permission.isEditor(false);

        String actual = endpoint.grantPermission(mockRequest, mockResponse, permission);
        String expected = "Permissions updated for " + USER_EMAIL;

        assertThat(actual, equalTo(expected));
        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).removeEditor(USER_EMAIL, session);
        verify(permissionsService, times(1)).removeAdministrator(USER_EMAIL, session);
    }
}
