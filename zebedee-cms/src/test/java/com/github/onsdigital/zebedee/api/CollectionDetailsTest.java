package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionDetail;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReaderSupplier;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ContentDeleteService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

public class CollectionDetailsTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ZebedeeCmsService zebedeeCmsService;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private ContentDeleteService contentDeleteService;

    @Mock
    private ZebedeeCollectionReaderSupplier collectionReaderSupplier;

    @Mock
    private com.github.onsdigital.zebedee.model.Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private Session session;

    private CollectionDetails collectionDetails;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(zebedeeCmsService.getSession()).thenReturn(session);
        when(zebedeeCmsService.getPermissions()).thenReturn(permissionsService);

        when(collection.getDescription()).thenReturn(description);
        when(description.getId()).thenReturn("test-collection-id");
        when(description.getType()).thenReturn(CollectionType.manual);

        when(permissionsService.canView(session, "test-collection-id", CollectionType.manual))
                .thenReturn(true);

        collectionDetails = new CollectionDetails(
                contentDeleteService,
                collectionReaderSupplier,
                zebedeeCmsService,
                false
        );
    }

    @Test
    public void get_collectionNotFound_shouldReturn404() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request)).thenReturn(null);

            CollectionDetail result = collectionDetails.get(request, response);

            assertThat(result, is(nullValue()));
            verify(response, times(1)).setStatus(HttpStatus.NOT_FOUND_404);
        }
    }

    @Test
    public void get_permissionDenied_shouldReturn401() throws Exception {
        when(permissionsService.canView(session, "test-collection-id", CollectionType.manual))
                .thenReturn(false);

        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request)).thenReturn(collection);

            CollectionDetail result = collectionDetails.get(request, response);

            assertThat(result, is(nullValue()));
            verify(response, times(1)).setStatus(HttpStatus.UNAUTHORIZED_401);
            verify(permissionsService, times(1)).canView(session, "test-collection-id", CollectionType.manual);
        }
    }

    @Test
    public void get_canViewThrowsIOException_shouldReturn401() throws Exception {
        when(permissionsService.canView(session, "test-collection-id", CollectionType.manual))
                .thenThrow(new IOException("permissions error"));

        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request)).thenReturn(collection);

            IOException ex = assertThrows(IOException.class, () -> collectionDetails.get(request, response));

            assertThat(ex.getMessage(), is(equalTo("permissions error")));
        }
    }
}