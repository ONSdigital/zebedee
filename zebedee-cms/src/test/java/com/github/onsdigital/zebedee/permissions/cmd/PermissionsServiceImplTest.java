package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PermissionsServiceImplTest {

    static final String SESSION_ID = "217"; // The Overlook Hotel room 217...
    static Optional<String> SESS = Optional.of(SESSION_ID);

    @Mock
    private Collections collectionsService;

    @Mock
    private SessionsService sessionsService;

    @Mock
    private ServiceStore serviceStore;

    @Mock
    private CollectionPermissionsService collectionPermissionsService;

    @Mock
    private Session session;

    private PermissionsServiceImpl service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        service = new PermissionsServiceImpl(sessionsService, collectionsService, serviceStore,
                collectionPermissionsService);
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_sessionIDNull() throws Exception {
        try {
            service.getSession(Optional.empty());
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));

            verifyZeroInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_sessionNotFound() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(null);

        try {
            service.getSession(SESS);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verifyNoMoreInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_IOException() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenThrow(new IOException(""));

        try {
            service.getSession(SESS);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verifyNoMoreInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_expired() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);

        when(sessionsService.expired(session))
                .thenReturn(true);

        try {
            service.getSession(SESS);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verify(sessionsService, times(1)).expired(session);
            throw ex;
        }
    }

    @Test
    public void testGetSession_success() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);

        when(sessionsService.expired(session))
                .thenReturn(false);

        Session result = service.getSession(SESS);
        assertThat(result, equalTo(session));

        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
    }
}
