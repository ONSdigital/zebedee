package com.github.onsdigital.zebedee.session.store;


import com.github.onsdigital.zebedee.session.store.exceptions.*;
import com.github.onsdigital.exceptions.JWTDecodeException;
import com.github.onsdigital.exceptions.JWTTokenExpiredException;
import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.interfaces.JWTHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;


public class SessionThreadLocalTest {

    private static final Class<SessionsKeyException> CLASS = SessionsKeyException.class;
    private SessionsThreadLocal sessionsStore;
    private String secretKey;
    private final static String SIGNED_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fn_ojA25syD6ajJ6we_grfBpaPSUSQeVSqnQGAozkHA";
    private final static String SECRET_KEY = "my-HS256-bit-secret";
    private final static String TOKEN_NO_USER        = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCJ9.u0K17k2kNo8_UUYNvWrIYDambCL7cRwUESPmeUmA9JE";
    private final static String TOKEN_EXPIRED_TIME   = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5LCJpYXQiOjE1NjIxOTA1MjQsImp0aSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNsaWVudF9pZCI6IjU3Y2Jpc2hrNGoyNHBhYmMxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJqYW5lZG9lQGV4YW1wbGUuY29tIn0.isHbp66W4VL_uS3Zms_uH0nEvoe3yBEzDUuMvxYZwK8";
    private final static String INVALID_SIGNED_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhLS1iYmJiLWNjY2MtZGPvv71kLWVlZWVlZWVlZWVlZSIsImRldmljZV9rZXkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjb2duaXRvOmdyb3VwcyI6WyJhZG1pbiIsInB1Ymxpc2hpbmciLCJkYXRhIiwidGVzdCJdLCJ0b2tlbl91c2UiOiJhY2Nlc3MiLCJzY29wZSI6ImF3cy5jb2duaXRvLnNpZ25pbi51c2VyLmFkbWluIiwiYXV0aF90aW1lIjoxNTYyMTkwNTI0LCJpc3MiOiJodHRwczovL2NvZ25pdG8taWRwLnVzLXdlc3QtMi5hbWF6b25hd3MuY29tL3VzLXdlc3QtMl9leGFtcGxlIiwiZXhwIjo5OTk5OTk5OTk5LCJpYXQiOjE1NjIxOTA1MjQsImp0aSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNsaWVudF9pZCI6IjU3Y2Jpc2hrNGoyNHBhYmMxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJqYW5lZG9lQGV4YW1wbGUuY29tIn0.G-hL9kNQ8kVDmPpJDMeQzF8tDdR8yLOudaxqc1Ij0RQ";


    @Mock
    private HttpServletRequest request;

    @Mock
    private JWTHandler jwtHandler;

    @Mock
    private UserDataPayload userDataPayload;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.sessionsStore = new SessionsThreadLocalImpl(request, SECRET_KEY);

    }

    @Test 
    public void createThreadLocal() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(SIGNED_TOKEN);
        when(jwtHandler.verifyJWT(any(),any())).thenReturn(userDataPayload);

        sessionsStore.store(request,SECRET_KEY);
        ThreadLocal<UserDataPayload> localThread = SessionsThreadLocalImpl.getStore();
        UserDataPayload localPayLoad = localThread.get();

        Arrays.sort(localPayLoad.getGroups());
        assertThat(localPayLoad,is(notNullValue()));
        assertThat(localPayLoad.getEmail(), is("\"janedoe@example.com\""));
        assertThat(localPayLoad.getGroups()[0], is("admin"));
        assertThat(localPayLoad.getGroups()[1], is("data"));
        assertThat(localPayLoad.getGroups()[2], is("publishing"));
        assertThat(localPayLoad.getGroups()[3], is("test"));

    }

    @Test 
    public void SessionThreadlocalKeyException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(SIGNED_TOKEN);
        assertThrows(
            SessionsKeyException.class,
            new ThrowingRunnable() {
             public void run() throws Throwable {
                sessionsStore.store(request,secretKey);
             }
            });
         }

    @Test 
    public void SessionThreadlocalRequestException() throws Exception {

        assertThrows(
            SessionsRequestException.class,
            new ThrowingRunnable() {
            public void run() throws Throwable {
                sessionsStore.store(request,SECRET_KEY);
            }
            });
        }
     
    @Test 
    public void SessionThreadlocalDecodeException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(TOKEN_NO_USER);
        when(jwtHandler.verifyJWT(any(),any())).thenThrow(JWTDecodeException.class);
            
        assertThrows(
            SessionsDecodeException.class,
            new ThrowingRunnable() {
            public void run() throws Throwable {
                sessionsStore.store(request,SECRET_KEY);
            }
            });
        }
    @Test 
    public void SessionThreadlocalTokenExired() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(TOKEN_EXPIRED_TIME);
        when(jwtHandler.verifyJWT(any(),any())).thenThrow(JWTTokenExpiredException.class);
            
        assertThrows(
            SessionsTokenExpiredException.class,
            new ThrowingRunnable() {
            public void run() throws Throwable {
                sessionsStore.store(request,SECRET_KEY);
            }
            });
        }

    @Test 
    public void SessionVerificationException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(INVALID_SIGNED_TOKEN);
        when(jwtHandler.verifyJWT(any(),any())).thenThrow(JWTVerificationException.class);
            
        assertThrows(
            SessionsVerificationException.class,
            new ThrowingRunnable() {
            public void run() throws Throwable {
                sessionsStore.store(request,SECRET_KEY);
            }
            });
        }


}
