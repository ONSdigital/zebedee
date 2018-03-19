package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.Matchers.any;
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
    private SessionsService sessionsService;

    @Before
    public void setUp() throws Exception {
        api = new Service();

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(api, "serviceStore", serviceStore);
        ReflectionTestUtils.setField(api, "permissionsService", permissionsService);
        ReflectionTestUtils.setField(api, "sessionsService", sessionsService);
    }

    @Test
    public void createNewServiceAccountTest() throws IOException, NotFoundException, UnauthorizedException {
        Session session = new Session();
        session.setEmail("other@ons.gov.uk");
        session.setId("123");
        PermissionDefinition permissionDefinition = new PermissionDefinition();
        permissionDefinition.admin = true;

        when(sessionsService.get(mockRequest)).thenReturn(session);
        when(permissionsService.userPermissions(session.getEmail(), session)).thenReturn(permissionDefinition);
        when(serviceStore.store(Mockito.anyString(), any())).thenReturn(new ServiceAccount("123"));
        api.createService(mockRequest, mockResponse);
        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).userPermissions(session.getEmail(), session);
        verify(serviceStore, times(1)).store(Mockito.anyString(),any());
        verify(mockResponse).setStatus(HttpServletResponse.SC_CREATED);
    }

    @Test
    public void createNewServiceAccountWithNoAdminRights() throws IOException, NotFoundException, UnauthorizedException {
        Session session = new Session();
        session.setEmail("other@ons.gov.uk");
        session.setId("123");
        PermissionDefinition permissionDefinition = new PermissionDefinition();
        permissionDefinition.admin = false;

        when(sessionsService.get(mockRequest)).thenReturn(session);
        when(permissionsService.userPermissions(session.getEmail(), session)).thenReturn(permissionDefinition);
        when(serviceStore.store(Mockito.anyString(), any())).thenReturn(new ServiceAccount("123"));
        api.createService(mockRequest, mockResponse);
        verify(sessionsService, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).userPermissions(session.getEmail(), session);
        verify(serviceStore, times(0)).store(Mockito.anyString(),any());
        verify(mockResponse).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
