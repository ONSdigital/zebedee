package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.json.ResultMessage;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CompleteTest {

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

    private Complete completeEndpoint;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        completeEndpoint = new Complete();

        Root.zebedee = zebedee;
        when(zebedee.getSessions()).thenReturn(sessions);
        when(zebedee.getCollections()).thenReturn(collections);
        when(sessions.get()).thenReturn(session);
        when(session.getEmail()).thenReturn("test@ons.gov.uk");
        when(request.getParameter("uri")).thenReturn("/test-uri");
    }

    @Test
    public void complete_shouldGetWritableCollectionAndCloseOnSuccess() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);

            ResultMessage result = completeEndpoint.complete(request, response);

            assertThat(result.message, is(equalTo("URI reviewed.")));
            collectionsApi.verify(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true));
            verify(collections).complete(collection, "/test-uri", session, false);
            verify(collection).close();
        }
    }

    @Test
    public void complete_recursiveTrue_shouldPassTrueToCollectionsService() throws Exception {
        when(request.getParameter("recursive")).thenReturn("true");

        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);

            completeEndpoint.complete(request, response);

            verify(collections).complete(collection, "/test-uri", session, true);
            verify(collection).close();
        }
    }

    @Test
    public void complete_shouldCloseCollectionWhenCompleteThrows() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            doThrow(new ConflictException("complete failed")).when(collections).complete(collection, "/test-uri", session, false);

            assertThrows(ConflictException.class, () -> completeEndpoint.complete(request, response));

            collectionsApi.verify(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true));
            verify(collection).close();
        }
    }
}
