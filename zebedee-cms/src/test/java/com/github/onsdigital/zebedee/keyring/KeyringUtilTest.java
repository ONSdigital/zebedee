package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeyringUtilTest {

    static String EMAIL = "robertBobby@test.com";

    @Mock
    private UsersService usersService;

    @Mock
    private User user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getUser_userServiceNull_shouldThrowException() {
        InternalServerError ex = assertThrows(InternalServerError.class, () -> KeyringUtil.getUser(null, null));
        assertThat(ex.getMessage(), equalTo("get user requires non null userService"));
    }

    @Test
    public void getUser_userServiceError_shouldThrowException() throws Exception {
        when(usersService.getUserByEmail(any()))
                .thenThrow(IOException.class);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> KeyringUtil.getUser(usersService, EMAIL));

        assertThat(ex.getMessage(), equalTo("get user returned unexpected error"));
        verify(usersService, times(1)).getUserByEmail(EMAIL);
    }

    @Test
    public void getUser_userServiceReturnsNull_shouldThrowException() throws Exception {
        when(usersService.getUserByEmail(any()))
                .thenReturn(null);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> KeyringUtil.getUser(usersService, EMAIL));

        assertThat(ex.getMessage(), equalTo("requested user was not found"));
        verify(usersService, times(1)).getUserByEmail(EMAIL);
    }

    @Test
    public void getUser_success_shouldReturnUser() throws Exception {
        when(usersService.getUserByEmail(EMAIL))
                .thenReturn(user);

        User actual = KeyringUtil.getUser(usersService, EMAIL);

        assertThat(actual, equalTo(user));
        verify(usersService, times(1)).getUserByEmail(EMAIL);
    }
}
