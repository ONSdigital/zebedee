package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

public class ListKeyringTest extends ZebedeeAPIBaseTestCase {

    @Mock
    private CollectionKeyring centralKeyring;

    @Mock
    private Sessions sessions;

    @Mock
    private PermissionsService permissionsService;

    private ListKeyring endpoint;

    private String email;

    @Override
    protected void customSetUp() throws Exception {
        endpoint = new ListKeyring(centralKeyring, sessions, permissionsService);
        email = "123@test.com";

        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getParameter("email"))
                .thenReturn(email);

        when(permissionsService.isAdministrator(session))
                .thenReturn(true);
    }

    @Override
    protected Object getAPIName() {
        return "KeyringListTest";
    }

    @Test
    public void testGet_getSessionErr_shouldThrowInternalServerErrorException() throws Exception {
        when(sessions.get(mockRequest))
                .thenThrow(IOException.class);

        assertThrows(InternalServerError.class, () -> endpoint.listUserKeys(mockRequest, mockResponse));
    }

    @Test
    public void testGet_getSessionNull_shouldThrowUnauthorisedException() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> endpoint.listUserKeys(mockRequest, mockResponse));
    }

    @Test
    public void testGet_keyringListError_shouldThrowInternalServerErrorException() throws Exception {
        when(centralKeyring.list(session))
                .thenThrow(KeyringException.class);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.listUserKeys(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo("internal server error"));
    }

    @Test
    public void testGet_success_shouldReturnCollectionIds() throws Exception {
        Set<String> userKeys = new HashSet<>();
        userKeys.add("666");

        when(centralKeyring.list(session))
                .thenReturn(userKeys);

        Set<String> actual = endpoint.listUserKeys(mockRequest, mockResponse);

        assertThat(actual, equalTo(userKeys));
    }
}
