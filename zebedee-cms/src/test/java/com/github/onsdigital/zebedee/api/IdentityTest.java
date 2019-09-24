package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.authorisation.AuthorisationService;
import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.authorisation.UserIdentityException;
import com.github.onsdigital.zebedee.json.JSONable;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IdentityTest {

    private static final String FLORENCE_TOKEN = "666";
    private static final String FLORENCE_TOKEN_HEADER = "X-Florence-Token";
    private static final String AUTH_TOKEN = "Bearer d8b90a24c3d247aeaf84731e4e69dd6f";
    private static final String SERVICE_NAME = "dp-dataset-api";

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private PrintWriter printWriterMock;

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private ServiceStore serviceStore;

    private Identity api;

    @Before
    public void setUp() throws Exception {
        api = new Identity(true, serviceStore, authorisationService); // enable feature by default

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(api, "authorisationService", authorisationService);

        ReflectionTestUtils.setField(api, "serviceStore", serviceStore);
    }

    @Test
    public void shouldReturnUnauthorisedIfNoAuthTokenProvided() throws Exception {
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api.identifyUser(mockRequest, mockResponse);

        verifyZeroInteractions(authorisationService);
        verifyResponseInteractions(new Error("service not authenticated"), SC_UNAUTHORIZED);
    }

    @Test
    public void shouldReturnExpectedErrorStatusIfIdentifyUserFails() throws Exception {
        when(mockRequest.getHeader(FLORENCE_TOKEN_HEADER))
                .thenReturn(FLORENCE_TOKEN);
        when(authorisationService.identifyUser(FLORENCE_TOKEN))
                .thenThrow(new UserIdentityException("bang!", SC_FORBIDDEN));
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);
        when(serviceStore.get(Mockito.any())).thenReturn(new ServiceAccount(SERVICE_NAME));
        when(mockRequest.getHeader(Identity.AUTHORIZATION_HEADER)).thenReturn(AUTH_TOKEN);

        api.identifyUser(mockRequest, mockResponse);

        verify(authorisationService, times(1)).identifyUser(FLORENCE_TOKEN);
        verifyResponseInteractions(new Error("bang!"), SC_FORBIDDEN);
    }

    @Test
    public void shouldReturnIdentityUserAndOKResponseForSuccess() throws Exception {
        Session session = new Session();
        session.setEmail("dartagnan@strangerThings.com");
        session.setId(FLORENCE_TOKEN);


        UserIdentity identity = new UserIdentity(session);

        when(serviceStore.get(Mockito.any())).thenReturn(new ServiceAccount(SERVICE_NAME));
        when(mockRequest.getHeader(Identity.AUTHORIZATION_HEADER)).thenReturn(AUTH_TOKEN);
        when(mockRequest.getHeader(FLORENCE_TOKEN_HEADER)).thenReturn(FLORENCE_TOKEN);
        when(authorisationService.identifyUser(FLORENCE_TOKEN))
                .thenReturn(identity);
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api.identifyUser(mockRequest, mockResponse);

        verify(serviceStore, times(0)).get("123");
        verify(authorisationService, times(1)).identifyUser(FLORENCE_TOKEN);
        verifyResponseInteractions(identity, SC_OK);
    }

    @Test
    public void shouldReturnIdentityServiceAndOKResponseForSuccess() throws Exception {
        final ServiceAccount serviceAccount = new ServiceAccount(SERVICE_NAME);
        UserIdentity identity = new UserIdentity(serviceAccount.getID());
        when(serviceStore.get(Mockito.any())).thenReturn(serviceAccount);
        when(mockRequest.getHeader(Identity.AUTHORIZATION_HEADER)).thenReturn(AUTH_TOKEN);
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api.identifyUser(mockRequest, mockResponse);

        verify(serviceStore, times(1)).get("d8b90a24c3d247aeaf84731e4e69dd6f");
        verify(authorisationService, times(0)).identifyUser(FLORENCE_TOKEN);
        verifyResponseInteractions(identity, SC_OK);
    }

    @Test
    public void shouldReturnErrorAndUnathorizedResponseForNoHeader() throws Exception {
        final ServiceAccount serviceAccount = new ServiceAccount(SERVICE_NAME);
        UserIdentity identity = new UserIdentity(serviceAccount.getID());
        when(serviceStore.get(Mockito.any())).thenReturn(serviceAccount);
        when(mockRequest.getHeader(Identity.AUTHORIZATION_HEADER)).thenReturn(AUTH_TOKEN);
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api.identifyUser(mockRequest, mockResponse);

        verify(serviceStore, times(1)).get("d8b90a24c3d247aeaf84731e4e69dd6f");
        verify(authorisationService, times(0)).identifyUser(FLORENCE_TOKEN);
        verifyResponseInteractions(identity, SC_OK);
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOExIfFailsToWriteResponse() throws Exception {
        Session session = new Session();
        session.setEmail("dartagnan@strangerThings.com");
        session.setId(FLORENCE_TOKEN);

        UserIdentity identity = new UserIdentity(session);

        when(serviceStore.get(Mockito.any())).thenReturn(new ServiceAccount(SERVICE_NAME));
        when(mockRequest.getHeader(Identity.AUTHORIZATION_HEADER)).thenReturn(AUTH_TOKEN);
        when(mockRequest.getHeader(FLORENCE_TOKEN_HEADER))
                .thenReturn(FLORENCE_TOKEN);
        when(authorisationService.identifyUser(FLORENCE_TOKEN))
                .thenReturn(identity);
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);
        when(mockResponse.getWriter())
                .thenThrow(new IOException("BOOM!"));

        try {
            api.identifyUser(mockRequest, mockResponse);
        } catch (IOException e) {
            verify(authorisationService, times(1)).identifyUser(FLORENCE_TOKEN);
            verify(mockResponse, times(1)).getWriter();
            verify(mockResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.name());
            verify(mockResponse, times(1)).setContentType(APPLICATION_JSON);
            verifyZeroInteractions(printWriterMock);
            throw e;
        }
    }

    @Test
    public void shouldReturnNotFoundIfCMDFeatureNotEnabled() throws Exception {
        Session session = new Session();
        session.setEmail("dartagnan@strangerThings.com");
        session.setId(FLORENCE_TOKEN);

        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        UserIdentity identity = new UserIdentity(session);

        api = new Identity(false, serviceStore, authorisationService); //disable feature for this test case.
        api.identifyUser(mockRequest, mockResponse);

        verifyZeroInteractions(serviceStore, authorisationService);
        verifyResponseInteractions(new Error("Not found"), SC_NOT_FOUND);
    }

    @Test
    public void shouldReturnUnauthorizedIfBearPrefixMissing() throws Exception {
        when(mockRequest.getHeader(Identity.AUTHORIZATION_HEADER))
                .thenReturn("123");

        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api = new Identity(true, serviceStore, authorisationService);
        api.identifyUser(mockRequest, mockResponse);

        verifyZeroInteractions(serviceStore, authorisationService);
        verifyResponseInteractions(new Error("service not authenticated"), SC_UNAUTHORIZED);
    }

    private void verifyResponseInteractions(JSONable body, int statusCode) throws IOException {
        verify(mockResponse, times(1)).getWriter();
        verify(mockResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.name());
        verify(mockResponse, times(1)).setContentType(APPLICATION_JSON);
        verify(printWriterMock, times(1)).write(body.toJSON());
        verify(mockResponse, times(1)).setStatus(statusCode);
    }
}
