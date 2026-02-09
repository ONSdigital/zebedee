package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.UserDataPayload;
import com.github.onsdigital.dp.authorisation.permissions.PermissionChecker;
import com.github.onsdigital.zebedee.permissions.model.Permissions;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PermissionsServiceImplementationTest {

    private static final String USER_ID = "user-id";
    private static final String USER_EMAIL = "user@ons.gov.uk";
    private static final String COLLECTION_ID = "collection-id";

    private PermissionsServiceImplementation permissionsService;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private Session session;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        permissionsService = new PermissionsServiceImplementation("http://permissions-api");
        setPermissionChecker(permissionsService, permissionChecker);

        when(session.getId()).thenReturn(USER_ID);
        when(session.getEmail()).thenReturn(USER_EMAIL);
        when(session.getGroups()).thenReturn(Arrays.asList("group-1"));
    }

    @Test
    public void isPublisher_ShouldThrowUnsupportedOperation() throws Exception {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                permissionsService.isPublisher(session));

        assertEquals("Permissions API is enabled: isPublisher is no longer supported", exception.getMessage());
    }

    @Test
    public void addAdministrator_ShouldThrowUnsupportedOperation() throws Exception {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                permissionsService.addAdministrator(USER_EMAIL, session));

        assertEquals("Permissions API is enabled: addAdministrator is no longer supported", exception.getMessage());
    }

    @Test
    public void canEdit_ShouldReturnTrueWhenUserHasPermission() throws Exception {
        when(permissionChecker.hasPermission(any(UserDataPayload.class), eq(Permissions.LEGACY_EDIT), anyMap()))
                .thenReturn(true);

        assertTrue(permissionsService.canEdit(session));

        verify(permissionChecker, times(1))
                .hasPermission(any(UserDataPayload.class), eq(Permissions.LEGACY_EDIT), anyMap());
    }

    @Test
    public void canEdit_ShouldReturnFalseWhenPermissionCheckThrows() throws Exception {
        when(permissionChecker.hasPermission(any(UserDataPayload.class), eq(Permissions.LEGACY_EDIT), anyMap()))
                .thenThrow(new Exception("boom"));

        assertFalse(permissionsService.canEdit(session));
    }

    @Test
    public void canView_ShouldReturnTrueAndUseCollectionId() throws Exception {
        when(permissionChecker.hasPermission(any(UserDataPayload.class), eq(Permissions.LEGACY_READ), anyMap()))
                .thenReturn(true);

        assertTrue(permissionsService.canView(session, COLLECTION_ID));

        ArgumentCaptor<Map<String, String>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(permissionChecker, times(1))
                .hasPermission(any(UserDataPayload.class), eq(Permissions.LEGACY_READ), attributesCaptor.capture());

        Map<String, String> attributes = attributesCaptor.getValue();
        assertEquals(COLLECTION_ID, attributes.get("collection_id"));
    }

    @Test
    public void canView_ShouldWrapExceptionInRuntimeException() throws Exception {
        when(permissionChecker.hasPermission(any(UserDataPayload.class), eq(Permissions.LEGACY_READ), anyMap()))
                .thenThrow(new Exception("boom"));

        assertThrows(RuntimeException.class, () -> permissionsService.canView(session, COLLECTION_ID));
    }

    private void setPermissionChecker(PermissionsServiceImplementation service, PermissionChecker checker) throws Exception {
        Field field = PermissionsServiceImplementation.class.getDeclaredField("permissionChecker");
        field.setAccessible(true);
        field.set(service, checker);
    }
}
