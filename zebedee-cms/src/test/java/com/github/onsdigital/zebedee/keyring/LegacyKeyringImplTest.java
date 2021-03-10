package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_PUT_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_SESSION_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.SESSION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LegacyKeyringImplTest {

    static final String TEST_EMAIL = "bertandernie@sesamestreet.com";

    @Mock
    private User user;

    @Mock
    private Session session;

    @Mock
    private com.github.onsdigital.zebedee.json.Keyring userKeyring;

    @Mock
    private Sessions sessionsService;

    @Mock
    private ApplicationKeys applicationKeys;


    @Mock
    private KeyringCache cache;

    private Keyring keyring;
    private KeyringException expectedEx;

    @Before

    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        expectedEx = new KeyringException("bork");

        when(user.getEmail())
                .thenReturn(TEST_EMAIL);

        when(user.keyring())
                .thenReturn(userKeyring);

        when(sessionsService.find(TEST_EMAIL))
                .thenReturn(session);

        keyring = new LegacyKeyringImpl(sessionsService, cache, applicationKeys);
    }

    @Test
    public void testPopulateFromUser_userNull_shouldThrowException() {
        // Given user is null

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testPopulateFromUser_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(user.getEmail())
                .thenReturn(null);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testPopulateFromUser_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testPopulateFromUser_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testPopulateFromUser_getSessionError_shouldThrowException() throws IOException {
        // Given get session throws an exception
        when(sessionsService.find(TEST_EMAIL))
                .thenThrow(expectedEx);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(GET_SESSION_ERR));
        assertThat(ex.getCause(), equalTo(expectedEx));
    }

    @Test
    public void testPopulateFromUser_getSessionReturnsNull_shouldThrowException() throws IOException {
        // Given get session returns null
        when(sessionsService.find(TEST_EMAIL))
                .thenReturn(null);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
    }

    @Test
    public void testPopulateFromUser_cachePutError_shouldThrowException() throws IOException {
        // Given cache put throws an exception
        doThrow(expectedEx)
                .when(cache)
                .put(user, session);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(CACHE_PUT_ERR));
        assertThat(ex.getCause(), equalTo(expectedEx));
    }

    @Test
    public void testPopulateFromUser_success_shouldPopulateCacheAndAppKeys() throws IOException {
        // Given a valid user

        // When populate from user is called
        keyring.populateFromUser(user);

        // Then the keyring cache is updated with the users keys
        verify(sessionsService, times(1)).find(TEST_EMAIL);
        verify(cache, times(1)).put(user, session);

        // And the applicate keys are updated from the user keyring
        verify(applicationKeys, times(1)).populateCacheFromUserKeyring(userKeyring);
    }


}
