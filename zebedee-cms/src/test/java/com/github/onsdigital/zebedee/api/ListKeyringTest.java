package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.keyring.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.KeyringUtil.GET_USER_ERR_FMT;
import static com.github.onsdigital.zebedee.keyring.KeyringUtil.USER_NOT_FOUND_ERR_FMT;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

public class ListKeyringTest extends ZebedeeAPIBaseTestCase {

    @Mock
    private Keyring keyring;

    @Mock
    private Sessions sessions;

    @Mock
    private UsersService usersService;

    @Mock
    private User user;

    @Mock
    private PermissionsService permissionsService;

    private ListKeyring endpoint;

    private String email;

    @Override
    protected void customSetUp() throws Exception {
        endpoint = new ListKeyring(keyring, sessions, permissionsService, usersService);
        email = "123@test.com";

        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getParameter("email"))
                .thenReturn(email);

        when(permissionsService.isAdministrator(session))
                .thenReturn(true);

        when(usersService.getUserByEmail(email))
                .thenReturn(user);
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
    public void testGet_insufficientPermissions_shouldThrowUnauthorisedException() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> endpoint.listUserKeys(mockRequest, mockResponse));
    }

    @Test
    public void testGet_checkPermissionsError_shouldThrowInternalServerErrorException() throws Exception {
        when(permissionsService.isAdministrator(session))
                .thenThrow(IOException.class);

        assertThrows(InternalServerError.class, () -> endpoint.listUserKeys(mockRequest, mockResponse));
    }

    @Test
    public void testGet_emailNull_shouldThrowBadRequestException() throws Exception {
        when(mockRequest.getParameter("email"))
                .thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> endpoint.listUserKeys(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo("user email required but was null/empty"));
    }

    @Test
    public void testGet_getUserError_shouldThrowInternalServerErrorException() throws Exception {
        when(usersService.getUserByEmail(email))
                .thenThrow(IOException.class);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.listUserKeys(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo(format(GET_USER_ERR_FMT, email)));
    }

    @Test
    public void testGet_getUserReturnsNull_shouldThrowNotFoundException() throws Exception {
        when(usersService.getUserByEmail(email))
                .thenReturn(null);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> endpoint.listUserKeys(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo(format(USER_NOT_FOUND_ERR_FMT, email)));
    }

    @Test
    public void testGet_keyringListError_shouldThrowInternalServerErrorException() throws Exception {
        when(keyring.list(user))
                .thenThrow(KeyringException.class);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.listUserKeys(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo("internal server error"));
    }

    @Test
    public void testGet_success_shouldReturnCollectionIds() throws Exception {
        Set<String> userKeys = new HashSet<>();
        userKeys.add("666");

        when(keyring.list(user))
                .thenReturn(userKeys);

        Set<String> actual = endpoint.listUserKeys(mockRequest, mockResponse);

        assertThat(actual, equalTo(userKeys));
    }
}
