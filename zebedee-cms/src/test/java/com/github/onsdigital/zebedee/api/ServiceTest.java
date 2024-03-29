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
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        api = new Service(); // enable the dataset import feature

        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(api, "serviceStore", serviceStore);
        ReflectionTestUtils.setField(api, "permissionsService", permissionsService);
        ReflectionTestUtils.setField(api, "sessions", sessions);
    }

    @Test
    public void createNewServiceAccountTest() throws IOException, NotFoundException, UnauthorizedException {
        Session session = new Session("123", "other@ons.gov.uk");

        when(sessions.get()).thenReturn(session);
        when(permissionsService.isAdministrator(session)).thenReturn(true);
        when(serviceStore.store(Mockito.anyString(), any())).thenReturn(new ServiceAccount("123"));
        when(mockResponse.getWriter()).thenReturn(printWriterMock);
        api.createService(mockRequest, mockResponse);
        verify(sessions, times(1)).get();
        verify(permissionsService, times(1)).isAdministrator(session);
        verify(serviceStore, times(1)).store(Mockito.anyString(), any());
        verify(mockResponse).setStatus(HttpServletResponse.SC_CREATED);
    }

    @Test
    public void createNewServiceAccountWithNoAdminRights() throws IOException, NotFoundException, UnauthorizedException {
        Session session = new Session("123", "other@ons.gov.uk");

        when(sessions.get()).thenReturn(session);
        when(permissionsService.isAdministrator(session)).thenReturn(false);
        when(serviceStore.store(Mockito.anyString(), any())).thenReturn(new ServiceAccount("123"));
        api.createService(mockRequest, mockResponse);
        verify(sessions, times(1)).get();
        verify(permissionsService, times(1)).isAdministrator(session);
        verify(serviceStore, times(0)).store(Mockito.anyString(), any());
        verify(mockResponse).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
