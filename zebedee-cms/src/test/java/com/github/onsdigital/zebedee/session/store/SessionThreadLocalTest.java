package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.exceptions.*;
import com.github.onsdigital.exceptions.SessionsTokenExpiredException;
import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.interfaces.JWTHandler;

import com.github.onsdigital.zebedee.session.store.SessionsThreadLocal;

import com.github.onsdigital.zebedee.session.store.SessionsThreadLocalImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


public class SessionThreadLocalTest {

    private SessionsThreadLocal sessionsStore;
    private String secretKey;
    private final static String SIGNED_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fn_ojA25syD6ajJ6we_grfBpaPSUSQeVSqnQGAozkHA";
    private final static String SECRET_KEY = "my-HS256-bit-secret";


    @Mock
    private HttpServletRequest request;

    @Mock
    private JWTHandler jwtHandler;

    @Mock
    private UserDataPayload userDataPayload;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.sessionsStore = new SessionsThreadLocalImpl(request, "");

    }

    @Test
    public void doSomething() throws Exception {
        when(jwtHandler.verifyJWT(any(),any())).thenReturn(userDataPayload);
        when(request.getHeader("Authorization")).thenReturn(SIGNED_TOKEN);
        sessionsStore.store(request,SECRET_KEY);
        ThreadLocal<UserDataPayload> localThread = SessionsThreadLocalImpl.getStore();
        UserDataPayload localPayLoad = localThread.get();
        assertThat(localPayLoad,is(notNullValue()));
        assertThat(localPayLoad, equalTo(userDataPayload));

    }

    @Test (expected=RuntimeException.class)
    public void doSomethingelse() throws Exception {
        when(jwtHandler.verifyJWT(any(),any())).thenThrow(SessionsTokenExpiredException.class);
        sessionsStore.store(request,secretKey);
        ThreadLocal<UserDataPayload> localThread = SessionsThreadLocalImpl.getStore();
        UserDataPayload localPayLoad = localThread.get();
        assertThat(localPayLoad,is(notNullValue()));
        assertThat(localPayLoad, equalTo(userDataPayload));

    }

}
