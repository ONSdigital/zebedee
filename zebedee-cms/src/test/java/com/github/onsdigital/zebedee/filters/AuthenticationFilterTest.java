package com.github.onsdigital.zebedee.filters;

import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.session.service.SessionsException;
import com.google.common.net.MediaType;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.nio.charset.StandardCharsets;

import static com.github.onsdigital.zebedee.reader.util.RequestUtils.AUTH_HEADER;
import static com.github.onsdigital.zebedee.reader.util.RequestUtils.BEARER_PREFIX;
import static com.github.onsdigital.zebedee.reader.util.RequestUtils.FLORENCE_TOKEN_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthenticationFilterTest extends ZebedeeTestBaseFixture {

    private static final String UNAUTHENTICATED_PATH  = "/health";
    private static final String AUTHENTICATED_PATH    = "/this/is/a/dummy/path";
    private static final String LEGACY_TOKEN          = "7be8cdc8f0b63603eb34490c2fcb91a0a2d01a9c292dd8baf397779a22d917d9";
    private static final String SERVICE_TOKEN         = "fc07ff99e25f809ac67d0968d0b2d42fb91b7d6438d6b53a58b533b22b740aa0";
    private static final String JWT_TOKEN             = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fC3P6jnpnhmOxdlw0u4nOhehz7dCXsqX7RvqI1gEC4wrJoE6rlKH1mo7lR16K-EXWdXRoeN0_z0PZQzo__xOprAsY2XSNOexOcIo3hoydx6CkGWGmNNsLp35iGY3DgW6SLpQsdGF8HicJ9D9KCTPXKAGmOrkX3t92WSCLiQXXuER9gndzC6oLMU0akvKDstoTfwLWeSsogOQBn7_lUqGaHC8T06ZR37n_eOgUGSXwSFuYbg1zcY2xK3tMh4Wo8TOrADOrfLg660scpXuu-oDf0PNdgpXGU318IK1R0A2LiqqJWIV1sDE88uuPcX9-xgKc0eUn6qABXM9qhEyr6MS6g";
    private static final String SOME_VALIDATION_ERROR = "some error saying invalid session";

    /**
     * Class under test
     */
    private AuthenticationFilter authenticationFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Sessions sessions;

    @Mock
    private ServletOutputStream outputStream;

    @Captor
    private ArgumentCaptor<byte[]> responseBodyCaptor;

    @Captor
    private ArgumentCaptor<Integer> responseBodyOffset;

    @Captor
    private ArgumentCaptor<Integer> responseBodyLength;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        authenticationFilter = new AuthenticationFilter(sessions);

        doNothing().when(response).setContentType(MediaType.JSON_UTF_8.toString());
        doNothing().when(response).setStatus(anyInt());
        when(response.getOutputStream()).thenReturn(outputStream);
        doNothing().when(sessions).resetThread();
    }

    @Test
    public void filter_shouldReturnTrue_whenTokenNotProvidedForOptionsRequest() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.OPTIONS);

        assertTrue(authenticationFilter.filter(request, response));
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldReturnUnauthorised_whenTokenNotProvidedForAuthenticatedPath() throws Exception {
        when(request.getPathInfo()).thenReturn(AUTHENTICATED_PATH);
        doThrow(new SessionsException(SOME_VALIDATION_ERROR)).when(sessions).set(null);

        assertFalse(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(null);
        verify(response, times(1)).setContentType(MediaType.JSON_UTF_8.toString());
        verify(response, times(1)).setStatus(HttpStatus.UNAUTHORIZED_401);
        verify(outputStream).write(responseBodyCaptor.capture(), responseBodyOffset.capture(), responseBodyLength.capture());

        String responseBody = new String(responseBodyCaptor.getValue(), responseBodyOffset.getValue(),
                responseBodyLength.getValue(), StandardCharsets.UTF_8);
        assertEquals(doubleQuoted(SOME_VALIDATION_ERROR), responseBody);
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldReturnUnauthorised_whenEmptyTokenProvidedForAuthenticatedPath() throws Exception {
        when(request.getHeader(FLORENCE_TOKEN_HEADER)).thenReturn("  ");
        when(request.getPathInfo()).thenReturn(AUTHENTICATED_PATH);
        doThrow(new SessionsException(SOME_VALIDATION_ERROR)).when(sessions).set(null);

        assertFalse(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(null);
        verify(response, times(1)).setContentType(MediaType.JSON_UTF_8.toString());
        verify(response, times(1)).setStatus(HttpStatus.UNAUTHORIZED_401);
        verify(outputStream).write(responseBodyCaptor.capture(), responseBodyOffset.capture(), responseBodyLength.capture());

        String responseBody = new String(responseBodyCaptor.getValue(), responseBodyOffset.getValue(),
                responseBodyLength.getValue(), StandardCharsets.UTF_8);
        assertEquals(doubleQuoted(SOME_VALIDATION_ERROR), responseBody);
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldReturnUnauthorised_whenInvalidTokenProvidedForAuthenticatedPath() throws Exception {
        when(request.getHeader(FLORENCE_TOKEN_HEADER)).thenReturn(LEGACY_TOKEN);
        when(request.getPathInfo()).thenReturn(AUTHENTICATED_PATH);
        doThrow(new SessionsException(SOME_VALIDATION_ERROR)).when(sessions).set(LEGACY_TOKEN);

        assertFalse(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(LEGACY_TOKEN);
        verify(response, times(1)).setContentType(MediaType.JSON_UTF_8.toString());
        verify(response, times(1)).setStatus(HttpStatus.UNAUTHORIZED_401);
        verify(outputStream).write(responseBodyCaptor.capture(), responseBodyOffset.capture(), responseBodyLength.capture());

        String responseBody = new String(responseBodyCaptor.getValue(), responseBodyOffset.getValue(),
                responseBodyLength.getValue(), StandardCharsets.UTF_8);
        assertEquals(doubleQuoted(SOME_VALIDATION_ERROR), responseBody);
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldReturnTrue_whenValidFlorenceTokenProvidedForAuthenticatedPath() throws Exception {
        when(request.getHeader(FLORENCE_TOKEN_HEADER)).thenReturn(LEGACY_TOKEN);
        when(request.getPathInfo()).thenReturn(AUTHENTICATED_PATH);
        doNothing().when(sessions).set(LEGACY_TOKEN);

        assertTrue(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(LEGACY_TOKEN);
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldReturnTrue_whenValidJWTTokenProvidedForAuthenticatedPath() throws Exception {
        when(request.getHeader(AUTH_HEADER)).thenReturn(BEARER_PREFIX + JWT_TOKEN);
        when(request.getPathInfo()).thenReturn(AUTHENTICATED_PATH);
        doNothing().when(sessions).set(JWT_TOKEN);

        assertTrue(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(JWT_TOKEN);
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldUseAuthToken_whenValidFlorenceAndAuthTokensProvided() throws Exception {
        when(request.getHeader(FLORENCE_TOKEN_HEADER)).thenReturn(BEARER_PREFIX + JWT_TOKEN);
        when(request.getHeader(AUTH_HEADER)).thenReturn(BEARER_PREFIX + SERVICE_TOKEN);
        when(request.getPathInfo()).thenReturn(AUTHENTICATED_PATH);
        doNothing().when(sessions).set(SERVICE_TOKEN);

        assertTrue(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(SERVICE_TOKEN);
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldReturnTrue_whenTokenNotProvidedForUnauthenticatedPath() throws Exception {
        when(request.getPathInfo()).thenReturn(UNAUTHENTICATED_PATH);
        doThrow(new SessionsException(SOME_VALIDATION_ERROR)).when(sessions).set(null);

        assertTrue(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(null);
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldReturnTrue_whenInvalidTokenProvidedForUnauthenticatedPath() throws Exception {
        when(request.getHeader(FLORENCE_TOKEN_HEADER)).thenReturn(LEGACY_TOKEN);
        when(request.getPathInfo()).thenReturn(UNAUTHENTICATED_PATH);
        doThrow(new SessionsException(SOME_VALIDATION_ERROR)).when(sessions).set(LEGACY_TOKEN);

        assertTrue(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(LEGACY_TOKEN);
        verify(sessions, times(1)).resetThread();
    }

    @Test
    public void filter_shouldReturnTrue_whenValidTokenProvidedForUnauthenticatedPath() throws Exception {
        when(request.getHeader(FLORENCE_TOKEN_HEADER)).thenReturn(LEGACY_TOKEN);
        when(request.getPathInfo()).thenReturn(UNAUTHENTICATED_PATH);
        doNothing().when(sessions).set(LEGACY_TOKEN);

        assertTrue(authenticationFilter.filter(request, response));

        verify(sessions, times(1)).set(LEGACY_TOKEN);
        verify(sessions, times(1)).resetThread();
    }

    private String doubleQuoted(String input) {
        return '"'+input+'"';
    }
}
