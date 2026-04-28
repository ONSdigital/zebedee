package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.DeleteMarkerJson;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ContentDeleteService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteContentTest {

    @Mock
    private ZebedeeCmsService zebedeeCmsService;

    @Mock
    private ContentDeleteService deleteService;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private Session session;

    private DeleteContent deleteContentEndpoint;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(DeleteContent.class, "zebedeeCmsService", zebedeeCmsService);
        ReflectionTestUtils.setField(DeleteContent.class, "deleteService", deleteService);

        deleteContentEndpoint = new DeleteContent();

        when(zebedeeCmsService.getSession()).thenReturn(session);
        when(zebedeeCmsService.getPermissions()).thenReturn(permissionsService);
        when(collection.getDescription()).thenReturn(description);
        when(description.getId()).thenReturn("123");
        when(permissionsService.canView(session, "123")).thenReturn(true);
    }

    @Test
    public void createDeleteMarker_shouldUseWritableCollectionAndCloseIt() throws Exception {
        DeleteMarkerJson json = new DeleteMarkerJson()
                .setCollectionId("123")
                .setUri("/content")
                .setTitle("title")
                .setUser("test@ons.gov.uk");
        when(zebedeeCmsService.getCollection("123", true)).thenReturn(collection);

        deleteContentEndpoint.createDeleteMarker(new MockHttpServletRequest(), new MockHttpServletResponse(), json);

        verify(zebedeeCmsService).getCollection("123", true);
        verify(deleteService).addDeleteMarkerToCollection(eq(session), eq(collection), any());
        verify(collection).close();
    }

    @Test
    public void removeDeleteMarker_shouldCloseWritableCollectionWhenUriMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(zebedeeCmsService.getCollection(request, true)).thenReturn(collection);

        deleteContentEndpoint.removeDeleteMarker(request, new MockHttpServletResponse());

        verify(zebedeeCmsService).getCollection(request, true);
        verify(deleteService, never()).cancelPendingDelete(eq(collection), eq(session), any());
        verify(collection).close();
    }
}
