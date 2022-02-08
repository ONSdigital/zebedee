package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.HEAD;
import java.io.IOException;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AuthorisationServiceImplTest {

    private static final String SESSION_ID = "666";

    @Mock
    private Sessions sessions;

    @Mock
    private UsersService usersService;

    @Mock
    private Collection collection;

    @Mock
    private ServiceAccount serviceAccount;

    private ServiceSupplier<Sessions> sessionsSupplier = () -> sessions;
    private ServiceSupplier<UsersService> userServiceSupplier = () -> usersService;

    private AuthorisationService service;
    private UserIdentityException notAuthenticatedEx;
    private UserIdentityException internalServerErrorEx;
    private UserIdentityException notFoundEx;
    private Session session;
    private CollectionDescription desc;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        notAuthenticatedEx = new UserIdentityException("user not authenticated", SC_UNAUTHORIZED);
        internalServerErrorEx = new UserIdentityException("internal server error", SC_INTERNAL_SERVER_ERROR);
        notFoundEx = new UserIdentityException("user does not exist", SC_NOT_FOUND);

        service = new AuthorisationServiceImpl();

        session = new Session();
        session.setEmail("rickSanchez@CitadelOfRicks.com");
        session.setId(SESSION_ID);

        desc = new CollectionDescription();
        desc.setId("666");

        ReflectionTestUtils.setField(service, "sessionsSupplier", sessionsSupplier);
        ReflectionTestUtils.setField(service, "userServiceSupplier", userServiceSupplier);
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnNotAuthorisedIfSessionIDNull() throws Exception {
        try {
            service.identifyUser(null);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(notAuthenticatedEx));
            verifyNoInteractions(sessions, usersService);
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnNotAuthorisedIfSessionIDEmpty() throws Exception {
        try {
            service.identifyUser("");
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(notAuthenticatedEx));
            verifyNoInteractions(sessions, usersService);
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnNotAuthenicatedIfSessionNotFound() throws Exception {
        when(sessions.get())
                .thenReturn(null);
        try {
            service.identifyUser(SESSION_ID);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(notAuthenticatedEx));
            verify(sessions, times(1)).get();
            verifyNoInteractions(usersService);
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnNotFoundIfUserDoesNotExist() throws Exception {
        when(sessions.get())
                .thenReturn(session);
        when(usersService.exists(session.getEmail()))
                .thenReturn(false);
        try {
            service.identifyUser(SESSION_ID);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(notFoundEx));
            verify(sessions, times(1)).get();
            verify(usersService, times(1)).exists(session.getEmail());
            throw ex;
        }
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnInternalServerErrorIfErrorCheckingUserExistance() throws Exception {
        when(sessions.get())
                .thenReturn(session);
        when(usersService.exists(session.getEmail()))
                .thenThrow(new IOException("something terrible happened!"));
        try {
            service.identifyUser(SESSION_ID);
        } catch (UserIdentityException ex) {
            assertThat(ex, equalTo(internalServerErrorEx));
            verify(sessions, times(1)).get();
            verify(usersService, times(1)).exists(session.getEmail());
            throw ex;
        }
    }

    @Test
    public void shouldReturnUserIdentityIfSessionAndUserExist() throws Exception {
        when(sessions.get())
                .thenReturn(session);
        when(usersService.exists(session.getEmail()))
                .thenReturn(true);

        UserIdentity expected = new UserIdentity(session);

        UserIdentity actual = service.identifyUser(SESSION_ID);

        assertThat(actual, equalTo(expected));
        verify(sessions, times(1)).get();
        verify(usersService, times(1)).exists(session.getEmail());
    }

}
