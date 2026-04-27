package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
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
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReviewTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Zebedee zebedee;

    @Mock
    private Sessions sessions;

    @Mock
    private Collection collection;

    @Mock
    private Session session;

    private Review api;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        api = new Review();

        Root.zebedee = zebedee;
        when(zebedee.getSessions()).thenReturn(sessions);
        when(sessions.get()).thenReturn(session);
        when(session.getEmail()).thenReturn("test@ons.gov.uk");
        when(request.getParameter("uri")).thenReturn("/test-uri");
    }

    @Test
    public void review_shouldGetWritableCollectionAndCloseOnSuccess() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);

            ResultMessage result = api.review(request, response);

            assertThat(result.message, is(equalTo("URI reviewed.")));
            collectionsApi.verify(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true));
            verify(collection).review(session, "/test-uri", false);
            verify(collection).save();
            verify(collection).close();
        }
    }

    @Test
    public void review_recursiveTrue_shouldPassTrueToCollectionReview() throws Exception {
        when(request.getParameter("recursive")).thenReturn("true");

        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);

            api.review(request, response);

            verify(collection).review(session, "/test-uri", true);
            verify(collection).close();
        }
    }

    @Test
    public void review_nullCollection_shouldThrowNotFound() {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(null);

            NotFoundException ex = assertThrows(NotFoundException.class, () -> api.review(request, response));

            assertThat(ex.getMessage(), is(equalTo("Collection not found")));
        }
    }

    @Test
    public void review_shouldCloseCollectionWhenSaveThrows() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            doThrow(new IOException("save failed")).when(collection).save();

            assertThrows(IOException.class, () -> api.review(request, response));

            verify(collection).close();
        }
    }

    @Test
    public void review_shouldCloseCollectionWhenReviewThrows() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            doThrow(new ConflictException("review failed")).when(collection).review(session, "/test-uri", false);

            assertThrows(ConflictException.class, () -> api.review(request, response));

            verify(collection).close();
        }
    }
}
