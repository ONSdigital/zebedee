package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.JWTHandlerImpl;
import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.interfaces.JWTHandler;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsDecodeException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsKeyException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsRequestException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsTokenExpiredException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsVerificationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

public class SessionThreadLocalTest {
    //Class to be tested
    private SessionsThreadLocal sessionsStore;

    // private String secretKey;
    private final static String SIGNED_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fn_ojA25syD6ajJ6we_grfBpaPSUSQeVSqnQGAozkHA";
    private final static String SECRET_KEY = "my-HS256-bit-secret";
    private final static String TOKEN_NO_USER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCJ9.u0K17k2kNo8_UUYNvWrIYDambCL7cRwUESPmeUmA9JE";
    private final static String TOKEN_EXPIRED_TIME = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5LCJpYXQiOjE1NjIxOTA1MjQsImp0aSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNsaWVudF9pZCI6IjU3Y2Jpc2hrNGoyNHBhYmMxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJqYW5lZG9lQGV4YW1wbGUuY29tIn0.isHbp66W4VL_uS3Zms_uH0nEvoe3yBEzDUuMvxYZwK8";
    private final static String INVALID_SIGNED_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhLS1iYmJiLWNjY2MtZGPvv71kLWVlZWVlZWVlZWVlZSIsImRldmljZV9rZXkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjb2duaXRvOmdyb3VwcyI6WyJhZG1pbiIsInB1Ymxpc2hpbmciLCJkYXRhIiwidGVzdCJdLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6ImF3cy5jb2duaXRvLnNpZ25pbi51c2VyLmFkbWluIiwiYXV0aF90aW1lIjoxNTYyMTkwNTI0LCJpc3MiOiJodHRwczovL2NvZ25pdG8taWRwLnVzLXdlc3QtMi5hbWF6b25hd3MuY29tL3VzLXdlc3QtMl9leGFtcGxlIiwiZXhwIjo5OTk5OTk5OTk5LCJpYXQiOjE1NjIxOTA1MjQsImp0aSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNsaWVudF9pZCI6IjU3Y2Jpc2hrNGoyNHBhYmMxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJqYW5lZG9lQGV4YW1wbGUuY29tIn0.G-hL9kNQ8kVDmPpJDMeQzF8tDdR8yLOudaxqc1Ij0RQ";

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDataPayload userDataPayload;

    private JWTHandler jwtHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        jwtHandler = new JWTHandlerImpl();

        this.sessionsStore = new SessionsThreadLocalImpl(jwtHandler);
    }

    @Test
    public void createThreadLocal() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(SIGNED_TOKEN);

        sessionsStore.store(request, SECRET_KEY);

        ThreadLocal<UserDataPayload> localThread = SessionsThreadLocalImpl.getStore();
        UserDataPayload localPayLoad = localThread.get();
        Arrays.sort(localPayLoad.getGroups());
        assertThat(localPayLoad, is(notNullValue()));
        assertThat(localPayLoad.getEmail(), is("\"janedoe@example.com\""));
        assertThat(localPayLoad.getGroups()[0], is("admin"));
        assertThat(localPayLoad.getGroups()[1], is("data"));
        assertThat(localPayLoad.getGroups()[2], is("publishing"));
        assertThat(localPayLoad.getGroups()[3], is("test"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void SessionThreadlocalDecodeException() throws Exception {


        when(request.getHeader("Authorization")).thenReturn(TOKEN_NO_USER);
        Exception exception = assertThrows(SessionsDecodeException.class, () -> sessionsStore.store(request, SECRET_KEY));
        assertThat(exception.getMessage(), is("Required JWT payload claim not found [username or cognito:groups]."));
    }

    @Test
    public void SessionThreadlocalKeyExceptionforEmpty() throws Exception {
        String secretKey = "";
        when(request.getHeader("Authorization")).thenReturn(SIGNED_TOKEN);
        Exception exception = assertThrows(SessionsKeyException.class, () -> sessionsStore.store(request, secretKey));
        assertThat(exception.getMessage(), is("Secret key value expected but was null or empty."));
    }

    @Test
    public void SessionThreadlocalKeyExceptionforNull() throws Exception {
        String secretKey = null;
        when(request.getHeader("Authorization")).thenReturn(SIGNED_TOKEN);
        Exception exception = assertThrows(SessionsKeyException.class, () -> sessionsStore.store(request, secretKey));
        assertThat(exception.getMessage(), is("Secret key value expected but was null or empty."));
    }

    @Test
    public void SessionThreadlocalRequestException() throws Exception {
        Exception exception = assertThrows(SessionsRequestException.class, () -> sessionsStore.store(request, SECRET_KEY));
        assertThat(exception.getMessage(), is("Authorization Header required but none provided."));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void SessionThreadlocalTokenExired() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(TOKEN_EXPIRED_TIME);
        Exception exception = assertThrows(SessionsTokenExpiredException.class, () -> sessionsStore.store(request, SECRET_KEY));
        assertThat(exception.getMessage(), is("JWT verification failed as token is expired."));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void SessionVerificationException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(INVALID_SIGNED_TOKEN);
        Exception exception = assertThrows(SessionsVerificationException.class, () -> sessionsStore.store(request, SECRET_KEY));
        assertThat(exception.getMessage(), is("Verification of JWT token integrity failed."));
    }

}