package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CollectionTest extends ZebedeeAPIBaseTestCase {

    @Mock
    private Sessions sessions;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private Collections collections;

    @Mock
    private com.github.onsdigital.zebedee.model.Collection collection;

    @Mock
    private CollectionDescription description;

    private Collection endpoint;

    @Override
    protected void customSetUp() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(collections.getCollectionId(mockRequest))
                .thenReturn(COLLECTION_ID);

        when(collections.getCollection(COLLECTION_ID))
                .thenReturn(collection);

        when(collection.getDescription())
                .thenReturn(description);

        this.endpoint = new Collection(sessions, permissionsService, collections);
    }

    @Override
    protected Object getAPIName() {
        return "Collection";
    }

    @Test
    public void testGet_getSessionException_shouldThrowException() throws Exception {
        when(sessions.get(mockRequest))
                .thenThrow(IOException.class);

        IOException actual = assertThrows(IOException.class, () -> endpoint.get(mockRequest, mockResponse));

        verify(sessions, times(1)).get(mockRequest);
    }

    @Test
    public void testGet_getSessionReturnsNull_shouldThrowException() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(null);

        UnauthorizedException actual = assertThrows(UnauthorizedException.class,
                () -> endpoint.get(mockRequest, mockResponse));

        assertThat(actual.getMessage(), equalTo("You are not authorised to view collections."));
        verify(sessions, times(1)).get(mockRequest);
    }

    @Test
    public void testGet_uriCollectionIDNotProvide_shouldThrowException() throws Exception {
        when(collections.getCollectionId(mockRequest))
                .thenReturn("");

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> endpoint.get(mockRequest, mockResponse));

        assertThat(actual.getMessage(), equalTo("collection ID required but was null/empty"));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, never()).getCollection(anyString());
    }

    @Test
    public void testGet_getCollectionError_shouldThrowException() throws Exception {
        when(collections.getCollection(COLLECTION_ID))
                .thenThrow(IOException.class);

        IOException actual = assertThrows(IOException.class,
                () -> endpoint.get(mockRequest, mockResponse));

        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
    }

    @Test
    public void testGet_getCollectionReturnsNull_shouldThrowException() throws Exception {
        when(collections.getCollection(COLLECTION_ID))
                .thenReturn(null);

        NotFoundException actual = assertThrows(NotFoundException.class,
                () -> endpoint.get(mockRequest, mockResponse));

        assertThat(actual.getMessage(), equalTo("The collection you are trying to get was not found."));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
    }

    @Test
    public void testGet_checkPermissionsError_shouldThrowException() throws Exception {
        when(permissionsService.canEdit(TEST_EMAIL, description))
                .thenThrow(IOException.class);

        UnauthorizedException actual = assertThrows(UnauthorizedException.class,
                () -> endpoint.get(mockRequest, mockResponse));

        assertThat(actual.getMessage(), equalTo("You are not authorised to view this collection"));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canView(TEST_EMAIL, description);
    }
}
