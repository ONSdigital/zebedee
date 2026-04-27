package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PublishTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ZebedeeCmsService zebedeeCmsService;

    @Mock
    private com.github.onsdigital.zebedee.model.Collections collections;

    @Mock
    private Collection collection;

    @Mock
    private Session session;

    private Publish api;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        api = new Publish();

        ReflectionTestUtils.setField(api, "zebedeeCmsService", zebedeeCmsService);
        when(zebedeeCmsService.getSession()).thenReturn(session);
        when(zebedeeCmsService.getCollections()).thenReturn(collections);
        when(session.getEmail()).thenReturn("test@ons.gov.uk");
    }

    @Test
    public void publish_shouldGetWritableCollectionAndCloseOnSuccess() throws Exception {
        when(request.getParameter("breakbeforefiletransfer")).thenReturn("true");
        when(request.getParameter("skipVerification")).thenReturn("true");

        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            when(collections.publish(collection, session, true, true)).thenReturn(true);

            boolean result = api.publish(request, response);

            org.hamcrest.MatcherAssert.assertThat(result, is(true));
            collectionsApi.verify(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true));
            verify(collections).publish(collection, session, true, true);
            verify(collection).close();
        }
    }

    @Test
    public void publish_shouldCloseCollectionWhenPublishThrows() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            doThrow(new ConflictException("publish failed")).when(collections).publish(collection, session, false, false);

            assertThrows(ConflictException.class, () -> api.publish(request, response));

            verify(collection).close();
        }
    }

    @Test
    public void publish_shouldCloseCollectionWhenPublishIoThrows() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);
            doThrow(new IOException("publish failed")).when(collections).publish(collection, session, false, false);

            assertThrows(IOException.class, () -> api.publish(request, response));

            verify(collection).close();
        }
    }
}
