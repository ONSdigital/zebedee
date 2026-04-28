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

public class ContentMoveTest {

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

    private ContentMove contentMoveEndpoint;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        contentMoveEndpoint = new ContentMove();

        Root.zebedee = zebedee;
        when(zebedee.getSessions()).thenReturn(sessions);
        when(zebedee.getCollections()).thenReturn(collections);
        when(sessions.get()).thenReturn(session);
        when(session.getEmail()).thenReturn("test@ons.gov.uk");
        when(request.getParameter("uri")).thenReturn("/from-uri");
        when(request.getParameter("toUri")).thenReturn("/to-uri");
    }

    @Test
    public void moveContent_shouldGetWritableCollectionAndCloseOnSuccess() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);

            boolean result = contentMoveEndpoint.MoveContent(request, response);

            org.hamcrest.MatcherAssert.assertThat(result, is(true));
            collectionsApi.verify(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true));
            verify(collections).moveContent(session, collection, "/from-uri", "/to-uri");
            verify(collection).close();
        }
    }

    @Test
    public void moveContent_shouldCloseCollectionWhenMoveThrows() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            doThrow(new ConflictException("move failed")).when(collections).moveContent(session, collection, "/from-uri", "/to-uri");

            assertThrows(ConflictException.class, () -> contentMoveEndpoint.MoveContent(request, response));

            verify(collection).close();
        }
    }
}
