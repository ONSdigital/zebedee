package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UnlockTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Zebedee zebedee;

    @Mock
    private Sessions sessions;

    @Mock
    private com.github.onsdigital.zebedee.model.Collections collections;

    @Mock
    private Collection collection;

    @Mock
    private Session session;

    private Unlock unlockEndpoint;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        unlockEndpoint = new Unlock();

        Root.zebedee = zebedee;
        when(zebedee.getSessions()).thenReturn(sessions);
        when(zebedee.getCollections()).thenReturn(collections);
        when(sessions.get()).thenReturn(session);
        when(session.getEmail()).thenReturn("test@ons.gov.uk");
    }

    @Test
    public void unlockCollection_shouldGetWritableCollectionAndCloseOnSuccess() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            when(collections.unlock(collection, session)).thenReturn(true);

            boolean result = unlockEndpoint.unlockCollection(request, response);

            org.hamcrest.MatcherAssert.assertThat(result, is(true));
            collectionsApi.verify(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true));
            verify(collections).unlock(collection, session);
            verify(collection).close();
        }
    }

    @Test
    public void unlockCollection_falseResult_shouldStillCloseCollection() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            when(collections.unlock(collection, session)).thenReturn(false);

            boolean result = unlockEndpoint.unlockCollection(request, response);

            org.hamcrest.MatcherAssert.assertThat(result, is(false));
            verify(collection).close();
        }
    }

    @Test
    public void unlockCollection_shouldCloseCollectionWhenUnlockThrows() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            doThrow(new ConflictException("unlock failed")).when(collections).unlock(collection, session);

            assertThrows(ConflictException.class, () -> unlockEndpoint.unlockCollection(request, response));

            verify(collection).close();
        }
    }
}
