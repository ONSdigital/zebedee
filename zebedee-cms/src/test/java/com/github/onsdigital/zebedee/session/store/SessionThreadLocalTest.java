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
    private SessionsThreadLocalImpl sessionsStore;
    
    private static final String SIGNED_TOKEN         = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fC3P6jnpnhmOxdlw0u4nOhehz7dCXsqX7RvqI1gEC4wrJoE6rlKH1mo7lR16K-EXWdXRoeN0_z0PZQzo__xOprAsY2XSNOexOcIo3hoydx6CkGWGmNNsLp35iGY3DgW6SLpQsdGF8HicJ9D9KCTPXKAGmOrkX3t92WSCLiQXXuER9gndzC6oLMU0akvKDstoTfwLWeSsogOQBn7_lUqGaHC8T06ZR37n_eOgUGSXwSFuYbg1zcY2xK3tMh4Wo8TOrADOrfLg660scpXuu-oDf0PNdgpXGU318IK1R0A2LiqqJWIV1sDE88uuPcX9-xgKc0eUn6qABXM9qhEyr6MS6g";
    private final static String TOKEN_NO_USER        = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCJ9.Vy6CJLdgDsCpoExm79aZh-2ugrO5u8M4M2g6s65-4RcocXxN5FZaQFvibwdh9h4bbz_qXqxJloBgZq3PmrIZrCIllmHhIbRmc3IISPG5_fdVspcjwVLUWLw-dWbdqaMo2uP6JIFmUx6DenO8ZB5I-82woyqhRxqfiCKG5q-ZEos4PzYO8bWcxYSOtC-j9p9bHJHxCUjwNvNHwSPUKrLacoo7e0dmpQI90PqK1KZqp52iieKdrHRYgHrmcTmiXY2mV2Ul8RodDl04jWvUwd52Qn4nIo-qUxROfnf5jbY1-rNotK-B3n5MSFA0YHcuiGN-bt8dUCyLLKkYjqBRpalzlg";
    private final static String TOKEN_EXPIRED_TIME   = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6MTYyNTEzNzUzLCJpYXQiOjE1NjIxOTA1MjQsImp0aSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNsaWVudF9pZCI6IjU3Y2Jpc2hrNGoyNHBhYmMxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJqYW5lZG9lQGV4YW1wbGUuY29tIn0.S-s8RJNTY2czRGpKLGmMb7fPgdmB086diMtc7M7eXPmjj4DCFkH0Pn9quqy3VEzPUp0NpKWmlVZlaZf0dyAhld7wIUYD8csMD7pMOE9zJMBw3elc9TZJnV06nA63-Htv_ykNvp-nuU1GzewIh_ujIV0RyPRbcxnxF8p2_kWuTnqvaZ6kt1M-XNuHt3lVDj9yAJFeApeZEdrB2-ma3sAsupHuvMQ2JFPvTKz0jWp_7oKi-O21M66TmBiNzpcZJFc7_S9oFHHuy0lW6C_kEI8yQMUPEewVhXwE6doJPHQj-v6j6xc0ieOyDwXpyRUmItapZyDTVF0hkawtw4h5vmvNNw";
    private final static String INVALID_SIGNED_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhLS1iYmJiLWNjY2MtZGPvv71kLWVlZWVlZWVlZWVlZSIsImPzv712aWNlX2tleSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNvZ25pdG86Z3JvdXBzIjpbImFkbWluIiwicHVibGlzaGluZyIsImRhdGEiLCJ0ZXN0Il0sInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1NjIxOTA1MjQsImlzcyI6Imh0dHBzOi8vY29nbml0by1pZHAudXMtd2VzdC0yLmFtYXpvbmF3cy5jb20vdXMtd2VzdC0yX2V4YW1wbGUiLCJleHAiOjk5OTk5OTk5OTksImlhdCI6MTU2MjE5MDUyNCwianRpIjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY2xpZW50X2lkIjoiNTdjYmlzaGs0ajI0cGFiYzEyMzQ1Njc4OTAiLCJ1c2VybmFtZSI6ImphbmVkb2VAZXhhbXBsZS5jb20ifQ.C5gKr5nVH1-ytJ9bAf996c5d7n1NLRsBi0wLYMoODJr36Kq-0fcQRC-l_2HAjnNmmj-FL8w8MJaIFkwt5rC4X5safj_LAELfJXgSukoDXhbeEZyyr5bG9gBkt92c7SKdyfHo19qNxZWt1b3xBAinrzBc9gv2khpII-Qc75s4FBOPiXIf1020gOirCW8Hzmj6Hpa2pCJHO4nuVqtJea-L8YVRw6Gc4d-DJQcDGH1tR8l1ynSI1e_8v8e0nNWkUxsBPAAUfb1jg61-YDCXylQhsEkLUfqSYNseRUtQMw2j4VEzHwu6f_P_g67-587Zp-FiATy_S2cwZdsIl_Ga9f0xrA";

    private static final String PUBLIC_SIGNING_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv"
    +"vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc"
    +"aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy"
    +"tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0"
    +"e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb"
    +"V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9"
    +"MwIDAQAB";

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

        sessionsStore.store(request, PUBLIC_SIGNING_KEY);

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
        Exception exception = assertThrows(SessionsDecodeException.class, () -> sessionsStore.store(request, PUBLIC_SIGNING_KEY));
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
        Exception exception = assertThrows(SessionsRequestException.class, () -> sessionsStore.store(request, PUBLIC_SIGNING_KEY));
        assertThat(exception.getMessage(), is("Authorization Header required but none provided."));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void SessionThreadlocalTokenExired() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(TOKEN_EXPIRED_TIME);
        Exception exception = assertThrows(SessionsTokenExpiredException.class, () -> sessionsStore.store(request, PUBLIC_SIGNING_KEY));
        assertThat(exception.getMessage(), is("JWT verification failed as token is expired."));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void SessionVerificationException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(INVALID_SIGNED_TOKEN);
        Exception exception = assertThrows(SessionsVerificationException.class, () -> sessionsStore.store(request, PUBLIC_SIGNING_KEY));
        assertThat(exception.getMessage(), is("Verification of JWT token integrity failed."));
    }

}