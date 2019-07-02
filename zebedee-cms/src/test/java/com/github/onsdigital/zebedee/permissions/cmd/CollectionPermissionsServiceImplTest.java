package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CollectionPermissionsServiceImplTest {

    @Mock
    PermissionsService permissionsService;

    @Mock
    Session session;

    @Mock
    CollectionDescription description;

    CollectionPermissionsServiceImpl service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = new CollectionPermissionsServiceImpl(permissionsService);
    }

    @Test
    public void testHasEdit_trueSuccessful() throws Exception {
        when(permissionsService.canEdit(session)).thenReturn(true);

        assertTrue(service.hasEdit(session));

        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testHasEdit_falseSuccessful() throws Exception {
        when(permissionsService.canEdit(session)).thenReturn(false);

        assertFalse(service.hasEdit(session));

        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test(expected = PermissionsException.class)
    public void testHasEdit_IOException() throws Exception {
        when(permissionsService.canEdit(session)).thenThrow(new IOException());

        try {
            service.hasEdit(session);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(500));
            verify(permissionsService, times(1)).canEdit(session);
            throw ex;
        }
    }

    @Test
    public void testHasView_trueSuccessful() throws Exception {
        when(permissionsService.canView(session, description)).thenReturn(true);

        assertTrue(service.hasView(session, description));

        verify(permissionsService, times(1)).canView(session, description);
    }

    @Test
    public void testHasView_falseSuccessful() throws Exception {
        when(permissionsService.canView(session, description)).thenReturn(false);

        assertFalse(service.hasView(session, description));

        verify(permissionsService, times(1)).canView(session, description);
    }

    @Test(expected = PermissionsException.class)
    public void testHasView_IOException() throws Exception {
        when(permissionsService.canView(session, description)).thenThrow(new IOException());

        try {
            service.hasView(session, description);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(500));
            verify(permissionsService, times(1)).canView(session, description);
            throw ex;
        }
    }

}
