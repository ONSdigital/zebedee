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

    private static final String RESP_CONTENT_TYPE    = "application/json;charset=UTF-8";
    private static final String DUMMY_PATH           = "this/is/a/dummy/path";
    private static final String LOGIN_ERROR          = "\"Please log in\"";
    private static final String NO_AUTH_HEADER_FOUND = "\"No authorisation header found. Exiting...\"";
    private static final String SESSION_STORE_ERROR  = "\"Required JWT payload claim not found [username or cognito:groups].\"";
    private static final String SIGNED_TOKEN         = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fC3P6jnpnhmOxdlw0u4nOhehz7dCXsqX7RvqI1gEC4wrJoE6rlKH1mo7lR16K-EXWdXRoeN0_z0PZQzo__xOprAsY2XSNOexOcIo3hoydx6CkGWGmNNsLp35iGY3DgW6SLpQsdGF8HicJ9D9KCTPXKAGmOrkX3t92WSCLiQXXuER9gndzC6oLMU0akvKDstoTfwLWeSsogOQBn7_lUqGaHC8T06ZR37n_eOgUGSXwSFuYbg1zcY2xK3tMh4Wo8TOrADOrfLg660scpXuu-oDf0PNdgpXGU318IK1R0A2LiqqJWIV1sDE88uuPcX9-xgKc0eUn6qABXM9qhEyr6MS6g";
    private final static String TOKEN_NO_USER        = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCJ9.Vy6CJLdgDsCpoExm79aZh-2ugrO5u8M4M2g6s65-4RcocXxN5FZaQFvibwdh9h4bbz_qXqxJloBgZq3PmrIZrCIllmHhIbRmc3IISPG5_fdVspcjwVLUWLw-dWbdqaMo2uP6JIFmUx6DenO8ZB5I-82woyqhRxqfiCKG5q-ZEos4PzYO8bWcxYSOtC-j9p9bHJHxCUjwNvNHwSPUKrLacoo7e0dmpQI90PqK1KZqp52iieKdrHRYgHrmcTmiXY2mV2Ul8RodDl04jWvUwd52Qn4nIo-qUxROfnf5jbY1-rNotK-B3n5MSFA0YHcuiGN-bt8dUCyLLKkYjqBRpalzlg";
    private static final String RSA_KEY_ID_1         = "51GpotpSTlm+qc5xNYHsJJ6KkeObRf9XH41bHHKBI8M=";
    private static final String RSA_SIGNING_KEY_1    = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv"
    +"vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc"
    +"aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy"
    +"tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0"
    +"e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb"
    +"V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9"
    +"MwIDAQAB";

    private AuthenticationFilter authFilter;

    private boolean result;

    private JWTStore jwtStore;

    private JWTHandler jwtHandler = new JWTHandlerImpl();

    @Mock
    private Sessions sessions;

    @Mock
    private Session session;

    @Mock
    private Serialiser Serialiser = new Serialiser(); 

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        session = new Session();
        session.setId("123test-session-id");
        session.setEmail("other123@ons.gov.uk");
    }

    @Test
    public void authorisationFilterUsingLegacySessionsModelTest() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request   = new MockHttpServletRequest();
        request.setPathInfo(DUMMY_PATH);

        when(sessions.get(request)).thenReturn(session);

        authFilter = new AuthenticationFilter(false, this.sessions);
        
        result = authFilter.filter(request, response);
        assertEquals(result, true);
    }

    @Test
    public void authorisationFilterUsingLegacySessionsModelNoSessionFoundTest() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request   = new MockHttpServletRequest();
        request.setPathInfo(DUMMY_PATH);

        when(sessions.get(request)).thenReturn(null);

        authFilter = new AuthenticationFilter(false, this.sessions);
        result = authFilter.filter(request, response);

        String responseMessage = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        assertEquals(response.getContentType(), RESP_CONTENT_TYPE); 
        assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED_401);
        assertEquals(responseMessage, LOGIN_ERROR);
        assertEquals(result, false);
    }

    @Test
    public void authorisationFilterUsingThreadLocalSessionsModelTest() throws IOException {      
        Map<String, String> rsaKeyMap = new HashMap<String, String>();
        rsaKeyMap.put(RSA_KEY_ID_1, RSA_SIGNING_KEY_1);
        jwtStore = new JWTStore(jwtHandler, rsaKeyMap);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request   = new MockHttpServletRequest();
        request.addHeader("Authorization", SIGNED_TOKEN);
        request.setPathInfo(DUMMY_PATH);

        authFilter = new AuthenticationFilter(true, jwtStore);
        
        result = authFilter.filter(request, response);

        Session session = jwtStore.get();
        Arrays.sort(session.getGroups());
        assertThat(session, is(notNullValue()));
        assertThat(session.getEmail(), is("\"janedoe@example.com\""));
        assertThat(session.getGroups()[0], is("admin"));
        assertThat(session.getGroups()[1], is("data"));
        assertThat(session.getGroups()[2], is("publishing"));
        assertThat(session.getGroups()[3], is("test"));
        assertEquals(result, true);
    }

    @Test
    public void authorisationFilterUsingThreadLocalSessionsNoAuthorisationHeaderFoundTest() throws IOException {      
        Map<String, String> rsaKeyMap = new HashMap<String, String>();
        rsaKeyMap.put(RSA_KEY_ID_1, RSA_SIGNING_KEY_1);
        jwtStore = new JWTStore(jwtHandler, rsaKeyMap);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request   = new MockHttpServletRequest();
        request.setPathInfo(DUMMY_PATH);

        authFilter = new AuthenticationFilter(true, jwtStore);
        result = authFilter.filter(request, response);

        String responseMessage = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        assertEquals(response.getContentType(), RESP_CONTENT_TYPE); 
        assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST_400);
        assertEquals(responseMessage, NO_AUTH_HEADER_FOUND);
        assertEquals(result, false);
    }

    @Test
    public void authorisationFilterUsingThreadLocalSessionsVerificationErrorTest() throws IOException {      
        Map<String, String> rsaKeyMap = new HashMap<String, String>();
        rsaKeyMap.put(RSA_KEY_ID_1, RSA_SIGNING_KEY_1);
        jwtStore = new JWTStore(jwtHandler, rsaKeyMap);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request   = new MockHttpServletRequest();
        request.addHeader("Authorization", TOKEN_NO_USER);
        request.setPathInfo(DUMMY_PATH);

        authFilter = new AuthenticationFilter(true, jwtStore);
        result = authFilter.filter(request, response);

        String responseMessage = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        assertEquals(response.getContentType(), RESP_CONTENT_TYPE); 
        assertEquals(response.getStatus(), HttpStatus.INTERNAL_SERVER_ERROR_500);
        assertEquals(responseMessage, SESSION_STORE_ERROR);
        assertEquals(result, false);
    }
    
}
