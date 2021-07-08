package com.github.onsdigital.zebedee.filter;

import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import com.github.onsdigital.zebedee.filters.AuthenticationFilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;

import com.github.onsdigital.JWTHandlerImpl;
import com.github.onsdigital.interfaces.JWTHandler;

import com.github.onsdigital.zebedee.session.store.JWTStore;

import com.github.davidcarboni.restolino.json.Serialiser;

import org.eclipse.jetty.http.HttpStatus;

import java.nio.charset.StandardCharsets;

public class AuthenticationFilterTest extends ZebedeeTestBaseFixture {

    private static final String TEST_JWT_EMAIL             = "\"janedoe@example.com\"";
    private static final String TEST_JWT_GROUP_0           = "admin";
    private static final String TEST_JWT_GROUP_1           = "data";
    private static final String TEST_JWT_GROUP_2           = "publishing";
    private static final String TEST_JWT_GROUP_3           = "test";
    private static final String TEST_SESSION_ID            = "123test-session-id";
    private static final String TEST_USER_EMAIL            = "other123@ons.gov.uk";
    private static final String RESP_CONTENT_TYPE          = "application/json;charset=UTF-8";
    private static final String DUMMY_PATH                 = "this/is/a/dummy/path";
    private static final String LOGIN_ERROR                = "\"Please log in\"";
    private static final String NO_AUTH_HEADER_FOUND       = "\"No authorisation header found. Exiting...\"";
    private static final String SESSION_STORE_ERROR        = "\"Required JWT payload claim not found [username or cognito:groups].\"";
    private static final String ACCESS_TOKEN_EXPIRED_ERROR = "\"JWT verification failed as token is expired.\"";
    private static final String TOKEN_NOT_VALID_ERROR      = "\"Token format not valid.\"";
    private static final String SIGNED_TOKEN               = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fC3P6jnpnhmOxdlw0u4nOhehz7dCXsqX7RvqI1gEC4wrJoE6rlKH1mo7lR16K-EXWdXRoeN0_z0PZQzo__xOprAsY2XSNOexOcIo3hoydx6CkGWGmNNsLp35iGY3DgW6SLpQsdGF8HicJ9D9KCTPXKAGmOrkX3t92WSCLiQXXuER9gndzC6oLMU0akvKDstoTfwLWeSsogOQBn7_lUqGaHC8T06ZR37n_eOgUGSXwSFuYbg1zcY2xK3tMh4Wo8TOrADOrfLg660scpXuu-oDf0PNdgpXGU318IK1R0A2LiqqJWIV1sDE88uuPcX9-xgKc0eUn6qABXM9qhEyr6MS6g";
    private static final String MALFORMED_TOKEN            = "eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fC3P6jnpnhmOxdlw0u4nOhehz7dCXsqX7RvqI1gEC4wrJoE6rlKH1mo7lR16K-EXWdXRoeN0_z0PZQzo__xOprAsY2XSNOexOcIo3hoydx6CkGWGmNNsLp35iGY3DgW6SLpQsdGF8HicJ9D9KCTPXKAGmOrkX3t92WSCLiQXXuER9gndzC6oLMU0akvKDstoTfwLWeSsogOQBn7_lUqGaHC8T06ZR37n_eOgUGSXwSFuYbg1zcY2xK3tMh4Wo8TOrADOrfLg660scpXuu-oDf0PNdgpXGU318IK1R0A2LiqqJWIV1sDE88uuPcX9-xgKc0eUn6qABXM9qhEyr6MS6g";
    private static final String TOKEN_NO_USER              = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCJ9.Vy6CJLdgDsCpoExm79aZh-2ugrO5u8M4M2g6s65-4RcocXxN5FZaQFvibwdh9h4bbz_qXqxJloBgZq3PmrIZrCIllmHhIbRmc3IISPG5_fdVspcjwVLUWLw-dWbdqaMo2uP6JIFmUx6DenO8ZB5I-82woyqhRxqfiCKG5q-ZEos4PzYO8bWcxYSOtC-j9p9bHJHxCUjwNvNHwSPUKrLacoo7e0dmpQI90PqK1KZqp52iieKdrHRYgHrmcTmiXY2mV2Ul8RodDl04jWvUwd52Qn4nIo-qUxROfnf5jbY1-rNotK-B3n5MSFA0YHcuiGN-bt8dUCyLLKkYjqBRpalzlg";
    private static final String TOKEN_EXPIRED_TIME         = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6MTYyNTEzNzUzLCJpYXQiOjE1NjIxOTA1MjQsImp0aSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNsaWVudF9pZCI6IjU3Y2Jpc2hrNGoyNHBhYmMxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJqYW5lZG9lQGV4YW1wbGUuY29tIn0.S-s8RJNTY2czRGpKLGmMb7fPgdmB086diMtc7M7eXPmjj4DCFkH0Pn9quqy3VEzPUp0NpKWmlVZlaZf0dyAhld7wIUYD8csMD7pMOE9zJMBw3elc9TZJnV06nA63-Htv_ykNvp-nuU1GzewIh_ujIV0RyPRbcxnxF8p2_kWuTnqvaZ6kt1M-XNuHt3lVDj9yAJFeApeZEdrB2-ma3sAsupHuvMQ2JFPvTKz0jWp_7oKi-O21M66TmBiNzpcZJFc7_S9oFHHuy0lW6C_kEI8yQMUPEewVhXwE6doJPHQj-v6j6xc0ieOyDwXpyRUmItapZyDTVF0hkawtw4h5vmvNNw";
    private static final String RSA_KEY_ID_1               = "51GpotpSTlm+qc5xNYHsJJ6KkeObRf9XH41bHHKBI8M=";
    private static final String RSA_SIGNING_KEY_1          = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv"
    +"vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc"
    +"aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy"
    +"tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0"
    +"e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb"
    +"V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9"
    +"MwIDAQAB";

    private AuthenticationFilter authFilterOldModel;

    private AuthenticationFilter authFilterNewModel;

    private boolean result;

    private JWTStore jwtStore;

    private JWTHandler jwtHandler;

    private MockHttpServletResponse response;

    private MockHttpServletRequest request;

    private Map<String, String> rsaKeyMap;

    @Mock
    private Sessions sessions;

    @Mock
    private Session session;

    @Mock
    private Serialiser Serialiser = new Serialiser(); 

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        jwtHandler = new JWTHandlerImpl();
        response   = new MockHttpServletResponse();
        request    = new MockHttpServletRequest();
        rsaKeyMap  = new HashMap<String, String>();

        session = new Session();
        session.setId(TEST_SESSION_ID);
        session.setEmail(TEST_USER_EMAIL);

        request.setPathInfo(DUMMY_PATH);

        authFilterOldModel = new AuthenticationFilter(false, this.sessions);
        authFilterNewModel = new AuthenticationFilter(true, jwtStore);

        rsaKeyMap.put(RSA_KEY_ID_1, RSA_SIGNING_KEY_1);
        jwtStore = new JWTStore(jwtHandler, rsaKeyMap);
    }

    @Test
    public void authorisationFilterUsingLegacySessionsModelTrueTest() throws IOException {
        when(sessions.get(request)).thenReturn(session);
        
        result = authFilterOldModel.filter(request, response);
        assertEquals(result, true);
    }

    @Test
    public void authorisationFilterUsingLegacySessionsModelFalseTest() throws IOException {
        when(sessions.get(request)).thenReturn(null);
        
        result = authFilterOldModel.filter(request, response);
        assertEquals(result, false);
    }

    @Test
    public void authorisationFilterUsingLegacySessionsModelNoSessionFoundTest() throws IOException {
        when(sessions.get(request)).thenReturn(null);

        result = authFilterOldModel.filter(request, response);

        String responseMessage = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        assertEquals(response.getContentType(), RESP_CONTENT_TYPE); 
        assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED_401);
        assertEquals(responseMessage, LOGIN_ERROR);
        assertEquals(result, false);
    }

    @Test
    public void authorisationFilterUsingThreadLocalSessionsModelTest() throws IOException {      
        request.addHeader("Authorization", SIGNED_TOKEN);
        
        result = authFilterNewModel.filter(request, response);

        Session session = jwtStore.get();
        Arrays.sort(session.getGroups());
        assertThat(session, is(notNullValue()));
        assertThat(session.getEmail(), is(TEST_JWT_EMAIL));
        assertThat(session.getGroups()[0], is(TEST_JWT_GROUP_0));
        assertThat(session.getGroups()[1], is(TEST_JWT_GROUP_1));
        assertThat(session.getGroups()[2], is(TEST_JWT_GROUP_2));
        assertThat(session.getGroups()[3], is(TEST_JWT_GROUP_3));
        assertEquals(result, true);
    }

    @Test
    public void authorisationFilterUsingThreadLocalSessionsNoAuthorisationHeaderFoundTest() throws IOException {      
        result = authFilterNewModel.filter(request, response);

        String responseMessage = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        assertEquals(response.getContentType(), RESP_CONTENT_TYPE); 
        assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED_401);
        assertEquals(responseMessage, NO_AUTH_HEADER_FOUND);
        assertEquals(result, false);
    }

    @Test
    public void authorisationFilterUsingThreadLocalSessionsVerificationErrorTest() throws IOException {      
        request.addHeader("Authorization", TOKEN_NO_USER);

        result = authFilterNewModel.filter(request, response);

        String responseMessage = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        assertEquals(response.getContentType(), RESP_CONTENT_TYPE); 
        assertEquals(response.getStatus(), HttpStatus.INTERNAL_SERVER_ERROR_500);
        assertEquals(responseMessage, SESSION_STORE_ERROR);
        assertEquals(result, false);
    }
    
    @Test
    public void authorisationFilterUsingThreadLocalTokenExpiredErrorTest() throws IOException {      
        request.addHeader("Authorization", TOKEN_EXPIRED_TIME);

        result = authFilterNewModel.filter(request, response);

        String responseMessage = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        assertEquals(response.getContentType(), RESP_CONTENT_TYPE); 
        assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED_401);
        assertEquals(responseMessage, ACCESS_TOKEN_EXPIRED_ERROR);
        assertEquals(result, false);
    }

    @Test
    public void authorisationFilterUsingThreadLocalMalformedTokenErrorTest() throws IOException {      
        request.addHeader("Authorization", MALFORMED_TOKEN);

        result = authFilterNewModel.filter(request, response);

        String responseMessage = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        assertEquals(response.getContentType(), RESP_CONTENT_TYPE); 
        assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED_401);
        assertEquals(responseMessage, TOKEN_NOT_VALID_ERROR);
        assertEquals(result, false);
    }
}
