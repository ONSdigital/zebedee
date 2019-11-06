package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
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

import static com.github.onsdigital.zebedee.api.Service.NOT_FOUND_ERR;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServiceTest {

    private Service api;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private ServiceStore serviceStore;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private Sessions sessions;

    @Mock
    private PrintWriter printWriterMock;

    @Before
    public void setUp() throws Exception {
        api = new Service(true); // enable the dataset import feature

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(api, "serviceStore", serviceStore);
        ReflectionTestUtils.setField(api, "permissionsService", permissionsService);
        ReflectionTestUtils.setField(api, "sessions", sessions);
    }

    @Test
    public void createNewServiceAccountTest() throws IOException, NotFoundException, UnauthorizedException {
        Session session = new Session();
        session.setEmail("other@ons.gov.uk");
        session.setId("123");

        when(sessions.get(mockRequest)).thenReturn(session);
        when(permissionsService.isAdministrator(session)).thenReturn(true);
        when(serviceStore.store(Mockito.anyString(), any())).thenReturn(new ServiceAccount("123"));
        when(mockResponse.getWriter()).thenReturn(printWriterMock);
        api.createService(mockRequest, mockResponse);
        verify(sessions, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).isAdministrator(session);
        verify(serviceStore, times(1)).store(Mockito.anyString(), any());
        verify(mockResponse).setStatus(HttpServletResponse.SC_CREATED);
    }

    @Test
    public void createNewServiceAccountWithNoAdminRights() throws IOException, NotFoundException, UnauthorizedException {
        Session session = new Session();
        session.setEmail("other@ons.gov.uk");
        session.setId("123");

        when(sessions.get(mockRequest)).thenReturn(session);
        when(permissionsService.isAdministrator(session)).thenReturn(false);
        when(serviceStore.store(Mockito.anyString(), any())).thenReturn(new ServiceAccount("123"));
        api.createService(mockRequest, mockResponse);
        verify(sessions, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).isAdministrator(session);
        verify(serviceStore, times(0)).store(Mockito.anyString(), any());
        verify(mockResponse).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void shouldReturnNotFoundIfFeatureDisabled() throws Exception {
        Session session = new Session();
        session.setEmail("other@ons.gov.uk");
        session.setId("123");

        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api = new Service(false); // explicitly disable the feature for this test case.
        api.createService(mockRequest, mockResponse);

        verifyZeroInteractions(sessions, permissionsService, serviceStore);

        verify(mockResponse, times(1)).getWriter();
        verify(mockResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.name());
        verify(mockResponse, times(1)).setContentType(APPLICATION_JSON);
        verify(printWriterMock, times(1)).write(NOT_FOUND_ERR.toJSON());
        verify(mockResponse, times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}
