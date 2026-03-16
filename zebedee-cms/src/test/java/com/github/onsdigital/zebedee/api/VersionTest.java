package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

public class VersionTest extends ZebedeeAPIBaseTestCase {

    private static final CollectionType TEST_COLLECTION_TYPE = CollectionType.manual;

    @Mock
    private Zebedee zebedee;

    @Mock
    private Sessions sessions;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private Collections collections;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    private Version endpoint;

    @Override
    protected void customSetUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        endpoint = new Version();
        Root.zebedee = zebedee;

        when(zebedee.getSessions()).thenReturn(sessions);
        when(zebedee.getPermissionsService()).thenReturn(permissionsService);
        when(zebedee.getCollections()).thenReturn(collections);
        when(sessions.get()).thenReturn(mockSession);

        when(collections.getCollection(COLLECTION_ID)).thenReturn(collection);
        when(collection.getDescription()).thenReturn(description);
        when(description.getType()).thenReturn(TEST_COLLECTION_TYPE);
        when(mockRequest.getPathInfo()).thenReturn("/version/" + COLLECTION_ID);
        when(mockRequest.getParameter("uri")).thenReturn("/some/uri");
    }

    @Override
    protected Object getAPIName() {
        return "Version";
    }

    @Test
    public void create_permissionDeniedForCollectionType_shouldThrowUnauthorized() throws Exception {
        when(permissionsService.canEdit(mockSession)).thenReturn(true);
        when(permissionsService.canEdit(mockSession, TEST_COLLECTION_TYPE)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> endpoint.create(mockRequest, mockResponse));
    }

    @Test
    public void delete_permissionDeniedForCollectionType_shouldThrowForbidden() throws Exception {
        when(permissionsService.canEdit(mockSession)).thenReturn(true);
        when(permissionsService.canEdit(mockSession, TEST_COLLECTION_TYPE)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> endpoint.delete(mockRequest, mockResponse));
    }
}
