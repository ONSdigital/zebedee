package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
        when(sessionsService.get(mockRequest))
                .thenReturn(session);

        when(usersService.getUserByEmail(TEST_EMAIL))
                .thenReturn(srcUser);

        when(usersService.getUserByEmail(USER_EMAIL))
                .thenReturn(targetUser);

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
    public void testGrant_addAdminAssignSuccess_shouldAssignPermissionsAndKeys() throws Exception {
        when(permissionsService.canView(session, collectionDescription))
                .thenReturn(true);

        permission.isAdmin(true);

        endpoint.grantPermission(mockRequest, mockResponse, permission);

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
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
    public void testGrant_removeEditorError_shouldRemoveAdminAndEditorPermissions() throws Exception {
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
