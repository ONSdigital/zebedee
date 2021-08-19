package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Mock
    private CollectionKeyring collectionKeyring;

    @Mock
    private Collection.ScheduleCanceller scheduleCanceller;

    @Mock
    private UsersService usersService;

    @Mock
    private User user;

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

        when(permissionsService.canView(TEST_EMAIL, description))
                .thenReturn(true);

        when(permissionsService.canEdit(TEST_EMAIL))
                .thenReturn(true);

        when(description.getName())
                .thenReturn("test");

        this.endpoint = new Collection(sessions, permissionsService, collections, usersService, collectionKeyring,
                scheduleCanceller);
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
        when(permissionsService.canView(TEST_EMAIL, description))
                .thenThrow(IOException.class);

        UnauthorizedException actual = assertThrows(UnauthorizedException.class,
                () -> endpoint.get(mockRequest, mockResponse));

        assertThat(actual.getMessage(), equalTo("You are not authorised to view this collection"));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canView(TEST_EMAIL, description);
    }

    @Test
    public void testGet_permissionDenied_shouldThrowException() throws Exception {
        when(permissionsService.canView(TEST_EMAIL, description))
                .thenReturn(false);

        UnauthorizedException actual = assertThrows(UnauthorizedException.class,
                () -> endpoint.get(mockRequest, mockResponse));

        assertThat(actual.getMessage(), equalTo("You are not authorised to view this collection"));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canView(TEST_EMAIL, description);
    }

    @Test
    public void testGet_success_shouldThrowException() throws Exception {
        CollectionDescription actual = endpoint.get(mockRequest, mockResponse);

        assertThat(actual, is(notNullValue()));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canView(TEST_EMAIL, description);
    }

    @Test
    public void testPost_sessionServiceError_shouldThrowException() throws Exception {
        when(sessions.get(mockRequest))
                .thenThrow(IOException.class);

        assertThrows(IOException.class, () -> endpoint.create(mockRequest, mockResponse, description));

        verify(sessions, times(1)).get(mockRequest);
    }

    @Test
    public void testPost_sessionServiceReturnsNull_shouldThrowException() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> endpoint.create(mockRequest, mockResponse, description));

        assertThat(ex.getMessage(), equalTo("You are not authorised to create collections."));
        verify(sessions, times(1)).get(mockRequest);
    }

    @Test
    public void testPost_collectionNameNull_shouldReturnBadRequest() throws Exception {
        when(description.getName())
                .thenReturn(null);

        endpoint.create(mockRequest, mockResponse, description);

        verify(sessions, times(1)).get(mockRequest);
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_BAD_REQUEST);
        verifyNoMoreInteractions(sessions, permissionsService, collections);
    }

    @Test
    public void testPost_collectionNameEmpty_shouldReturnBadRequest() throws Exception {
        when(description.getName())
                .thenReturn("");

        endpoint.create(mockRequest, mockResponse, description);

        verify(sessions, times(1)).get(mockRequest);
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_BAD_REQUEST);
        verifyNoMoreInteractions(sessions, permissionsService, collections);
    }

    @Test
    public void testPost_permissionsCheckError_shouldThrowException() throws Exception {
        when(permissionsService.canEdit(TEST_EMAIL))
                .thenThrow(IOException.class);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> endpoint.create(mockRequest, mockResponse, description));

        assertThat(ex.getMessage(), equalTo("You are not authorised to edit collections."));
        verify(sessions, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).canEdit(TEST_EMAIL);
        verifyNoMoreInteractions(sessions, permissionsService, collections);
    }

    @Test
    public void testPost_permissionDenied_shouldThrowException() throws Exception {
        when(permissionsService.canEdit(TEST_EMAIL))
                .thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> endpoint.create(mockRequest, mockResponse, description));

        assertThat(ex.getMessage(), equalTo("You are not authorised to edit collections."));
        verify(sessions, times(1)).get(mockRequest);
        verify(permissionsService, times(1)).canEdit(TEST_EMAIL);
        verifyNoMoreInteractions(sessions, permissionsService, collections);
    }

    @Test
    public void testDelete_sessionNull_shouldThrowEx() throws Exception {
        when(sessions.get(any(HttpServletRequest.class)))
                .thenReturn(null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> endpoint.deleteCollection(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo("You are not authorised to delete collections."));
        verify(sessions, times(1)).get(mockRequest);
    }

    @Test
    public void testDelete_getSessionError_shouldThrowEx() throws Exception {
        when(sessions.get(any(HttpServletRequest.class)))
                .thenThrow(IOException.class);

        assertThrows(IOException.class, () -> endpoint.deleteCollection(mockRequest, mockResponse));

        verify(sessions, times(1)).get(mockRequest);
    }

    @Test
    public void testDelete_getCollectionError_shouldThrowEx() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getPathInfo())
                .thenReturn("collections/1234");

        when(collections.getCollection("1234"))
                .thenThrow(IOException.class);

        assertThrows(IOException.class, () -> endpoint.deleteCollection(mockRequest, mockResponse));

        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection("1234");
    }

    @Test
    public void testDelete_collectionNotFound_shouldThrowEx() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getPathInfo())
                .thenReturn("collections/1234");

        when(collections.getCollection("1234"))
                .thenReturn(null);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> endpoint.deleteCollection(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo("The collection you are trying to delete was not found"));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection("1234");
    }

    @Test
    public void testDelete_deleteCollectionError_shouldThrowEx() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getPathInfo())
                .thenReturn("collections/" + COLLECTION_ID);

        when(collections.getCollection(COLLECTION_ID))
                .thenReturn(collection);

        when(collection.getId())
                .thenReturn(COLLECTION_ID);

        doThrow(IOException.class)
                .when(collections)
                .delete(collection, session);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.deleteCollection(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo(format("error attempting to delete collection: {0}", COLLECTION_ID)));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
        verify(collections, times(1)).delete(collection, session);
    }

    @Test
    public void testDelete_getUserError_shouldThrowEx() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getPathInfo())
                .thenReturn("collections/" + COLLECTION_ID);

        when(collections.getCollection(COLLECTION_ID))
                .thenReturn(collection);

        when(collection.getId())
                .thenReturn(COLLECTION_ID);

        when(usersService.getUserByEmail(TEST_EMAIL))
                .thenThrow(IOException.class);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.deleteCollection(mockRequest, mockResponse));

        assertThat(ex.getMessage(),
                equalTo(format("error attempting to get user from session details: {0}", TEST_EMAIL)));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
        verify(collections, times(1)).delete(collection, session);
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);
    }

    @Test
    public void testDelete_getUserReturnsNull_shouldThrowEx() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getPathInfo())
                .thenReturn("collections/" + COLLECTION_ID);

        when(collections.getCollection(COLLECTION_ID))
                .thenReturn(collection);

        when(collection.getId())
                .thenReturn(COLLECTION_ID);

        when(usersService.getUserByEmail(TEST_EMAIL))
                .thenReturn(null);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.deleteCollection(mockRequest, mockResponse));

        assertThat(ex.getMessage(),
                equalTo(format("error attempting to get user from session details: {0}", TEST_EMAIL)));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
        verify(collections, times(1)).delete(collection, session);
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);
    }

    @Test
    public void testDelete_keyringRemoveError_shouldThrowEx() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getPathInfo())
                .thenReturn("collections/" + COLLECTION_ID);

        when(collections.getCollection(COLLECTION_ID))
                .thenReturn(collection);

        when(collection.getId())
                .thenReturn(COLLECTION_ID);

        when(usersService.getUserByEmail(TEST_EMAIL))
                .thenReturn(user);

        doThrow(KeyringException.class)
                .when(collectionKeyring)
                .remove(user, collection);

        InternalServerError ex = assertThrows(InternalServerError.class,
                () -> endpoint.deleteCollection(mockRequest, mockResponse));

        assertThat(ex.getMessage(), equalTo(format("error attempting to remove collection key from keyring: {0}", COLLECTION_ID)));
        verify(sessions, times(1)).get(mockRequest);
        verify(collections, times(1)).getCollection(COLLECTION_ID);
        verify(collections, times(1)).delete(collection, session);
        verify(usersService, times(1)).getUserByEmail(TEST_EMAIL);
        verify(collectionKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void testDelete_success_shouldDeleteCollection() throws Exception {
        when(sessions.get(mockRequest))
                .thenReturn(session);

        when(mockRequest.getPathInfo())
                .thenReturn("collections/1234");

        when(collections.getCollection("1234"))
                .thenReturn(collection);

        when(usersService.getUserByEmail(TEST_EMAIL))
                .thenReturn(user);

        boolean result = endpoint.deleteCollection(mockRequest, mockResponse);

        assertTrue(result);
        verify(collections, times(1)).delete(collection, session);
        verify(scheduleCanceller, times(1)).cancel(collection);
        verify(collectionKeyring, times(1)).remove(user, collection);
    }
}
