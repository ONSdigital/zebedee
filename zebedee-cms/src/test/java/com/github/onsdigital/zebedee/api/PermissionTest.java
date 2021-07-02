package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
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
    private Keyring keyring;

    @Mock
    private User srcUser, targetUser;

    @Mock
    private SecretKey key;

    @Captor
    private ArgumentCaptor<List<CollectionDescription>> removalsCaptor;

    @Captor
    private ArgumentCaptor<List<CollectionDescription>> assignmentsCaptor;

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

        when(keyring.get(any(), any()))
                .thenReturn(key);

        when(collectionMock.getDescription())
                .thenReturn(collectionDescription);

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
    public void testGrant_addAdminSrcUserNull_shouldThrowException() throws Exception {
        when(usersService.getUserByEmail(any()))
                .thenReturn(null);

        permission.isAdmin(true);

        assertThrows(NotFoundException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);
        verifyZeroInteractions(keyring);
    }

    @Test
    public void testGrant_addAdminTargetUserNull_shouldThrowException() throws Exception {
        when(usersService.getUserByEmail(TEST_EMAIL))
                .thenReturn(srcUser);

        when(usersService.getUserByEmail(USER_EMAIL))
                .thenReturn(null);

        permission.isAdmin(true);

        assertThrows(NotFoundException.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);
        verify(usersService, times(1)).getUserByEmail(USER_EMAIL);
        verifyZeroInteractions(keyring);
    }

    @Test
    public void testGrant_addAdminCollectionListError_shouldThrowException() throws Exception {
        when(collections.list())
                .thenThrow(IOException.class);

        permission.isAdmin(true);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        assertThat(ex.getMessage(), equalTo("error listing collections"));
        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(collections, times(1)).list();
        verifyZeroInteractions(keyring);
    }

    @Test
    public void testGrant_addAdminCanViewError_shouldThrowException() throws Exception {
        when(permissionsService.canView(eq(targetUser), any()))
                .thenThrow(IOException.class);

        permission.isAdmin(true);

        assertThrows(IOException.class, () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(permissionsService, times(1)).canView(eq(targetUser), any());
        verifyZeroInteractions(keyring);
    }

    @Test
    public void testGrant_addAdminRevokeFromToError_shouldThrowException() throws Exception {
        doThrow(KeyringException.class)
                .when(keyring)
                .revokeFrom(any(), any(List.class));

        permission.isAdmin(true);

        assertThrows(KeyringException.class, () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(permissionsService, times(1)).canView(eq(targetUser), any());

        verify(keyring, times(1)).revokeFrom(eq(targetUser), removalsCaptor.capture());
        List<CollectionDescription> removals = removalsCaptor.getValue();
        assertThat(removals, is(notNullValue()));
        assertThat(removals.size(), equalTo(1));
        assertThat(removals.get(0), equalTo(collectionDescription));
    }

    @Test
    public void testGrant_addAdminAssignToError_shouldThrowException() throws Exception {
        doThrow(KeyringException.class)
                .when(keyring)
                .assignTo(any(User.class), any(User.class), any(List.class));

        when(permissionsService.canView(targetUser, collectionDescription))
                .thenReturn(true);

        permission.isAdmin(true);

        assertThrows(KeyringException.class, () -> endpoint.grantPermission(mockRequest, mockResponse, permission));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(permissionsService, times(1)).canView(eq(targetUser), any());

        verify(keyring, times(1)).revokeFrom(eq(targetUser), removalsCaptor.capture());
        List<CollectionDescription> removals = removalsCaptor.getValue();
        assertThat(removals, is(notNullValue()));
        assertTrue(removals.isEmpty());

        verify(keyring, times(1)).assignTo(eq(srcUser), eq(targetUser), assignmentsCaptor.capture());
        List<CollectionDescription> asssignments = assignmentsCaptor.getValue();
        assertThat(asssignments, is(notNullValue()));
        assertThat(asssignments.size(), equalTo(1));
        assertThat(asssignments.get(0), equalTo(collectionDescription));
    }

    @Test
    public void testGrant_addAdminAssignSuccess_shouldAssignPermissionsAndKeys() throws Exception {
        when(permissionsService.canView(targetUser, collectionDescription))
                .thenReturn(true);

        permission.isAdmin(true);

        endpoint.grantPermission(mockRequest, mockResponse, permission);

        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).addAdministrator(USER_EMAIL, session);
        verify(permissionsService, times(1)).canView(eq(targetUser), any());

        verify(keyring, times(1)).revokeFrom(eq(targetUser), removalsCaptor.capture());
        List<CollectionDescription> removals = removalsCaptor.getValue();
        assertThat(removals, is(notNullValue()));
        assertTrue(removals.isEmpty());

        verify(keyring, times(1)).assignTo(eq(srcUser), eq(targetUser), assignmentsCaptor.capture());
        List<CollectionDescription> assignments = assignmentsCaptor.getValue();
        assertThat(assignments, is(notNullValue()));
        assertThat(assignments.size(), equalTo(1));
        assertThat(assignments.get(0), equalTo(collectionDescription));
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

        verify(keyring, times(1)).revokeFrom(eq(targetUser), removalsCaptor.capture());
        List<CollectionDescription> removals = removalsCaptor.getValue();
        assertThat(removals, is(notNullValue()));
        assertThat(removals.size(), equalTo(1));
        assertThat(removals.get(0), equalTo(collectionDescription));

        verify(keyring, times(1)).assignTo(eq(srcUser), eq(targetUser), assignmentsCaptor.capture());
        List<CollectionDescription> asssignments = assignmentsCaptor.getValue();
        assertThat(asssignments, is(notNullValue()));
        assertThat(asssignments.size(), equalTo(0));
    }
}
