package com.github.onsdigital.zebedee.reader.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class RequestUtilsTest {

    private static final String JWT_TOKEN     = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fC3P6jnpnhmOxdlw0u4nOhehz7dCXsqX7RvqI1gEC4wrJoE6rlKH1mo7lR16K-EXWdXRoeN0_z0PZQzo__xOprAsY2XSNOexOcIo3hoydx6CkGWGmNNsLp35iGY3DgW6SLpQsdGF8HicJ9D9KCTPXKAGmOrkX3t92WSCLiQXXuER9gndzC6oLMU0akvKDstoTfwLWeSsogOQBn7_lUqGaHC8T06ZR37n_eOgUGSXwSFuYbg1zcY2xK3tMh4Wo8TOrADOrfLg660scpXuu-oDf0PNdgpXGU318IK1R0A2LiqqJWIV1sDE88uuPcX9-xgKc0eUn6qABXM9qhEyr6MS6g";
    private static final String LEGACY_TOKEN  = "7be8cdc8f0b63603eb34490c2fcb91a0a2d01a9c292dd8baf397779a22d917d9";
    private static final String SERVICE_TOKEN = "166edec62e10881f1f735f350b7f787e85269259eb0a3e201f3614fa764f9030";
    private static final String BEARER        = "Bearer %s";

    @Mock
    private HttpServletRequest mockRequest;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getSessionId_ShouldBeNull_WhenRequestIsNull() throws Exception {
        assertThat(RequestUtils.getSessionId(null), is(nullValue()));
    }

    @Test
    public void getSessionId_ShouldBeEmpty_WhenBothHeadersEmpty() throws Exception {
        assertThat(RequestUtils.getSessionId(mockRequest), is(nullValue()));
    }

    @Test
    public void getSessionId_ShouldReturnToken_WhenLegacyFlorenceHeaderProvided() throws Exception {
        when(mockRequest.getHeader(RequestUtils.FLORENCE_TOKEN_HEADER))
                .thenReturn(LEGACY_TOKEN);

        assertThat(RequestUtils.getSessionId(mockRequest), is(LEGACY_TOKEN));
    }

    @Test
    public void getSessionId_ShouldReturnToken_WhenJWTFlorenceHeaderProvided() throws Exception {
        when(mockRequest.getHeader(RequestUtils.FLORENCE_TOKEN_HEADER))
                .thenReturn(JWT_TOKEN);

        assertThat(RequestUtils.getSessionId(mockRequest), is(JWT_TOKEN));
    }

    @Test
    public void getSessionId_ShouldBeEmpty_WhenFlorenceHeaderEmptyAndAuthHeaderNotJWT() throws Exception {
        when(mockRequest.getHeader(RequestUtils.AUTH_HEADER))
                .thenReturn(String.format(BEARER, SERVICE_TOKEN));

        assertThat(RequestUtils.getSessionId(mockRequest), is(SERVICE_TOKEN));
    }

    @Test
    public void getSessionId_ShouldReturnToken_WhenFlorenceHeaderEmptyAndAuthHeaderIsJWT() throws Exception {
        when(mockRequest.getHeader(RequestUtils.AUTH_HEADER))
                .thenReturn(String.format(BEARER, JWT_TOKEN));

        assertThat(RequestUtils.getSessionId(mockRequest), is(JWT_TOKEN));
    }
}
