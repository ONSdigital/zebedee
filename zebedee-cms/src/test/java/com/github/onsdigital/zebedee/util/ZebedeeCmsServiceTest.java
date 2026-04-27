package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZebedeeCmsServiceTest {

    @Mock
    private Zebedee zebedee;

    @Mock
    private com.github.onsdigital.zebedee.model.Collections modelCollections;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Collection collection;

    private ZebedeeCmsService service;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Root.zebedee = zebedee;
        service = ZebedeeCmsService.getInstance();
    }

    @Test
    public void getCollection_withRequestAndWriteable_shouldDelegateToApiCollections() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true)).thenReturn(collection);

            Collection actual = service.getCollection(request, true);

            assertThat(actual, is(collection));
            collectionsApi.verify(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, true));
        }
    }

    @Test
    public void getCollection_withRequest_shouldDefaultToReadOnly() throws Exception {
        try (MockedStatic<com.github.onsdigital.zebedee.api.Collections> collectionsApi = mockStatic(com.github.onsdigital.zebedee.api.Collections.class)) {
            collectionsApi.when(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, false)).thenReturn(collection);

            Collection actual = service.getCollection(request);

            assertThat(actual, is(collection));
            collectionsApi.verify(() -> com.github.onsdigital.zebedee.api.Collections.getCollection(request, false));
        }
    }

    @Test
    public void getCollection_withIdAndWriteable_shouldDelegateToModelCollections() throws Exception {
        when(zebedee.getCollections()).thenReturn(modelCollections);
        when(modelCollections.getCollection("123", true)).thenReturn(collection);

        Collection actual = service.getCollection("123", true);

        assertThat(actual, is(collection));
        verify(modelCollections).getCollection("123", true);
    }

    @Test
    public void getCollection_withId_shouldDefaultToReadOnly() throws Exception {
        when(zebedee.getCollections()).thenReturn(modelCollections);
        when(modelCollections.getCollection("123", false)).thenReturn(collection);

        Collection actual = service.getCollection("123");

        assertThat(actual, is(collection));
        verify(modelCollections).getCollection("123", false);
    }
}
