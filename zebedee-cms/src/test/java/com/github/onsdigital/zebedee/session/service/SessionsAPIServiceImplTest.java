package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.session.service.client.SessionClient;
import com.github.onsdigital.session.service.entities.SessionCreated;
import com.github.onsdigital.session.service.error.SessionClientException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SessionsAPIServiceImplTest {

    private static final String TEST_EMAIL = "test@ons.gov.uk";
    private static final String TEST_ID = "666";

    private Sessions sessions;

    @Mock
    private SessionClient sessionCli;

    @Mock
    private User user;

    @Mock
    private SessionCreated sessionCreated;

    @Mock
    private com.github.onsdigital.session.service.Session apiSession;

    private Session session;

    private Date date;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        date = new Date();

        when(user.getEmail())
                .thenReturn(TEST_EMAIL);

        when(sessionCreated.getId())
                .thenReturn(TEST_ID);

        when(apiSession.getId())
                .thenReturn(TEST_ID);

        when(apiSession.getEmail())
                .thenReturn(TEST_EMAIL);

        when(apiSession.getStart())
                .thenReturn(date);

        when(apiSession.getLastAccess())
                .thenReturn(date);

        sessions = new SessionsAPIServiceImpl(sessionCli);
    }

    @Test
    public void testCreate_nullUser_shouldThrowException() {
        IOException ex = assertThrows(IOException.class, () -> sessions.create(null));

        assertThat(ex.getMessage(), equalTo("create session requires user but was null"));
        verifyZeroInteractions(sessionCli);
    }

    @Test
    public void testCreate_userEmailNull_shouldThrowException() {
        when(user.getEmail()).thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("create session requires user email but was null or empty"));
        verifyZeroInteractions(sessionCli);
    }

    @Test
    public void testCreate_userEmailEmpty_shouldThrowException() {
        when(user.getEmail()).thenReturn("");

        IOException ex = assertThrows(IOException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("create session requires user email but was null or empty"));
        verifyZeroInteractions(sessionCli);
    }

    @Test
    public void testCreate_clientNewSessThrowsEx_shouldThrowException() {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenThrow(new SessionClientException("unexpected error"));

        SessionClientException ex = assertThrows(SessionClientException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("unexpected error"));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verifyNoMoreInteractions(sessionCli);
    }

    @Test
    public void testCreate_clientNewSessReturnsNull_shouldThrowException() {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("unexpected error creating new session expected session but was null"));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verifyNoMoreInteractions(sessionCli);
    }

    @Test
    public void testCreate_clientGetSessThrowEx_shouldThrowException() {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenReturn(sessionCreated);

        when(sessionCli.getSessionByID(TEST_ID))
                .thenThrow(new SessionClientException("Kapow"));

        SessionClientException ex = assertThrows(SessionClientException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("Kapow"));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testCreate_clientGetSessReturnNull_shouldThrowException() {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenReturn(sessionCreated);

        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("client failed to retrieve session from sessions api"));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testCreate_clientGetSessReturnsSessWithNullID_shouldThrowException() {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenReturn(sessionCreated);

        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        when(apiSession.getId())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty id"));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testCreate_clientGetSessReturnsSessWithEmptyID_shouldThrowException() {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenReturn(sessionCreated);

        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        when(apiSession.getId())
                .thenReturn("");

        IOException ex = assertThrows(IOException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty id"));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testCreate_clientGetSessReturnsSessWithNullEmail_shouldThrowException() {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenReturn(sessionCreated);

        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        when(apiSession.getEmail())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty email"));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testCreate_clientGetSessReturnsSessWithEmptyEmail_shouldThrowException() {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenReturn(sessionCreated);

        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        when(apiSession.getEmail())
                .thenReturn("");

        IOException ex = assertThrows(IOException.class, () -> sessions.create(user));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty email"));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testCreate_success_shouldCreateAndReturnNewSession() throws Exception {
        when(sessionCli.createNewSession(TEST_EMAIL))
                .thenReturn(sessionCreated);

        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        Session expected = Session.fromAPIModel(apiSession);

        Session actual = sessions.create(user);

        assertThat(actual, equalTo(expected));
        verify(sessionCli, times(1)).createNewSession(TEST_EMAIL);
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testGetByID_idNull_shouldThrowException() throws Exception {
        String id = null;

        Session actual = sessions.get(id);

        assertThat(actual, is(nullValue()));
        verifyZeroInteractions(sessionCli);
    }

    @Test
    public void testGetByID_idEmpty_shouldThrowException() throws Exception {
        Session actual = sessions.get("");

        assertThat(actual, is(nullValue()));
        verifyZeroInteractions(sessionCli);
    }

    @Test
    public void testGetByID_clientException_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByID(TEST_ID))
                .thenThrow(new SessionClientException("borked!"));

        SessionClientException ex = assertThrows(SessionClientException.class, () -> sessions.get(TEST_ID));

        assertThat(ex.getMessage(), equalTo("borked!"));
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testGetByID_clientReturnsNull_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(null);

        Session actual = sessions.get(TEST_ID);

        assertThat(actual, is(nullValue()));
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testGetByID_sessionIDNull_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        when(apiSession.getId())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.get(TEST_ID));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty id"));
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testGetByID_sessionIDEmpty_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        when(apiSession.getId())
                .thenReturn("");

        IOException ex = assertThrows(IOException.class, () -> sessions.get(TEST_ID));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty id"));
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testGetByID_sessionEmailNull_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        when(apiSession.getEmail())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.get(TEST_ID));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty email"));
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testGetByID_sessionEmailEmpty_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        when(apiSession.getEmail())
                .thenReturn("");

        IOException ex = assertThrows(IOException.class, () -> sessions.get(TEST_ID));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty email"));
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testGetByID_success_shouldReturnSession() throws Exception {
        when(sessionCli.getSessionByID(TEST_ID))
                .thenReturn(apiSession);

        Session expected = Session.fromAPIModel(apiSession);

        Session actual = sessions.get(TEST_ID);

        assertThat(actual, equalTo(expected));
        verify(sessionCli, times(1)).getSessionByID(TEST_ID);
    }

    @Test
    public void testFind_emailNull_shouldThrowException() throws Exception {
        String email = null;

        Session actual = sessions.find(email);

        assertThat(actual, is(nullValue()));
        verifyZeroInteractions(sessionCli);
    }


    @Test
    public void testFind_emailEmpty_shouldThrowException() throws Exception {
        Session actual = sessions.find("");

        assertThat(actual, is(nullValue()));
        verifyZeroInteractions(sessionCli);
    }

    @Test
    public void testFind_clientException_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByEmail(TEST_EMAIL))
                .thenThrow(new SessionClientException("borked!"));

        SessionClientException ex = assertThrows(SessionClientException.class, () -> sessions.find(TEST_EMAIL));

        assertThat(ex.getMessage(), equalTo("borked!"));
        verify(sessionCli, times(1)).getSessionByEmail(TEST_EMAIL);
    }

    @Test
    public void testFind_clientReturnsNull_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByEmail(TEST_EMAIL))
                .thenReturn(null);

        Session actual = sessions.find(TEST_EMAIL);

        assertThat(actual, is(nullValue()));
        verify(sessionCli, times(1)).getSessionByEmail(TEST_EMAIL);
    }

    @Test
    public void testFind_sessionIDNull_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByEmail(TEST_EMAIL))
                .thenReturn(apiSession);

        when(apiSession.getId())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.find(TEST_EMAIL));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty id"));
        verify(sessionCli, times(1)).getSessionByEmail(TEST_EMAIL);
    }

    @Test
    public void testFind_sessionIDEmpty_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByEmail(TEST_EMAIL))
                .thenReturn(apiSession);

        when(apiSession.getId())
                .thenReturn("");

        IOException ex = assertThrows(IOException.class, () -> sessions.find(TEST_EMAIL));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty id"));
        verify(sessionCli, times(1)).getSessionByEmail(TEST_EMAIL);
    }

    @Test
    public void testFind_sessionEmailNull_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByEmail(TEST_EMAIL))
                .thenReturn(apiSession);

        when(apiSession.getEmail())
                .thenReturn(null);

        IOException ex = assertThrows(IOException.class, () -> sessions.find(TEST_EMAIL));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty email"));
        verify(sessionCli, times(1)).getSessionByEmail(TEST_EMAIL);
    }

    @Test
    public void testFind_sessionEmailEmpty_shouldThrowException() throws Exception {
        when(sessionCli.getSessionByEmail(TEST_EMAIL))
                .thenReturn(apiSession);

        when(apiSession.getEmail())
                .thenReturn("");

        IOException ex = assertThrows(IOException.class, () -> sessions.find(TEST_EMAIL));

        assertThat(ex.getMessage(), equalTo("client has returned a session with a null/empty email"));
        verify(sessionCli, times(1)).getSessionByEmail(TEST_EMAIL);
    }

    @Test
    public void testFind_success_shouldReturnSession() throws Exception {
        when(sessionCli.getSessionByEmail(TEST_EMAIL))
                .thenReturn(apiSession);

        Session expected = Session.fromAPIModel(apiSession);

        Session actual = sessions.find(TEST_EMAIL);

        assertThat(actual, equalTo(expected));
        verify(sessionCli, times(1)).getSessionByEmail(TEST_EMAIL);
    }

    @Test
    public void testExpired_shouldReturnTrueForNullSession() {
        assertTrue(sessions.expired(null));
    }

    @Test
    public void testExpired_shouldReturnFalseWithSession() {
        assertFalse(sessions.expired(new Session()));
    }

}
