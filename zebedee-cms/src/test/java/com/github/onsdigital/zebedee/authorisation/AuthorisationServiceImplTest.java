package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AuthorisationServiceImplTest {

    @Mock
    private SessionsService sessionsService;

    @Mock
    private UsersService usersService;

    private AuthorisationService service;
    private ServiceSupplier<SessionsService> sessionServiceSupplier = () -> sessionsService;
    private ServiceSupplier<UsersService> userServiceSupplier = () -> usersService;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        service = new AuthorisationServiceImpl();

        ReflectionTestUtils.setField(service, "sessionServiceSupplier", sessionServiceSupplier);
        ReflectionTestUtils.setField(service, "userServiceSupplier", userServiceSupplier);
    }

    @Test(expected = UserIdentityException.class)
    public void shouldReturnErrorIfSessionIDNull() throws Exception {
        try {
            service.identifyUser(null);
        } catch (UserIdentityException e) {
            assertThat("", e.getMessage(), equalTo("sessionID is required but was null"));
            verifyZeroInteractions(sessionsService, usersService);
            throw e;
        }
    }

}
