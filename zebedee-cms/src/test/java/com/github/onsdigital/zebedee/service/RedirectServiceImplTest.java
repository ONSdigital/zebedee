package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dis.redirect.api.sdk.RedirectClient;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectAPIException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectNotFoundException;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionRedirect;
import com.github.onsdigital.zebedee.json.CollectionRedirectAction;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RedirectServiceImplTest {
    @Mock
    RedirectClient mockRedirectAPI;

    @Mock
    Collection mockCollection;

    @Mock
    CollectionDescription mockCollectionDescription;

    @Mock
    CollectionReader mockCollectionReader;

    @Mock
    ContentReader mockContentReader;

    @Mock
    Page mockPage;

    @Mock
    PageDescription mockDescription;

    private RedirectServiceImpl redirectService;

    private static final String TEST_COLLECTION_ID = "test-collection";

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        redirectService = new RedirectServiceImpl(mockRedirectAPI);
    }

    @Test
    public void testGenerateRedirectListForCollectionCreate() throws Exception {
        // Given a redirect API that doesn't have any redirects in it
        when(mockRedirectAPI.getRedirect(any())).thenThrow(new RedirectNotFoundException());;

        // And a collection with a migrationLink
        List<String> uris = Arrays.asList("/origin/data.json");
        when(mockDescription.getMigrationLink()).thenReturn("/destination");
        when(mockPage.getDescription()).thenReturn(mockDescription);
        when(mockContentReader.listUris()).thenReturn(uris);
        when(mockCollectionReader.getReviewed()).thenReturn(mockContentReader);
        when(mockContentReader.getContent("/origin")).thenReturn(mockPage);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        // When generateRedirectListForCollection is called
        redirectService.generateRedirectListForCollection(mockCollection, mockCollectionReader);

        // Then the expected CollectionRedirect should be added in the collection description 
        CollectionRedirect expectedCollectionRedirect = new CollectionRedirect("/origin", "/destination", CollectionRedirectAction.CREATE);
        verify(mockCollectionDescription, times(1)).addRedirect(expectedCollectionRedirect);
    }

    @Test
    public void testGenerateRedirectListForCollectionUpdate() throws Exception {
        // Given a redirect API that doesn't have any redirects in it
        when(mockRedirectAPI.getRedirect(any())).thenReturn(new Redirect("/origin", "/originaldestination"));

        // And a collection with a migrationLink
        List<String> uris = Arrays.asList("/origin/data.json");
        when(mockDescription.getMigrationLink()).thenReturn("/destination");
        when(mockPage.getDescription()).thenReturn(mockDescription);
        when(mockContentReader.listUris()).thenReturn(uris);
        when(mockCollectionReader.getReviewed()).thenReturn(mockContentReader);
        when(mockContentReader.getContent("/origin")).thenReturn(mockPage);

        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);


        // When generateRedirectListForCollection is called
        redirectService.generateRedirectListForCollection(mockCollection, mockCollectionReader);

        // Then the expected CollectionRedirect should be added in the collection description 
        CollectionRedirect expectedCollectionRedirect = new CollectionRedirect("/origin", "/destination", CollectionRedirectAction.UPDATE);
        verify(mockCollectionDescription, times(1)).addRedirect(expectedCollectionRedirect);
    }

    @Test
    public void testGenerateRedirectListForCollectionNoAction() throws Exception {
        // Given a redirect API that doesn't have any redirects in it
        when(mockRedirectAPI.getRedirect(any())).thenReturn(new Redirect("/origin", "/destination"));

        // And a collection with a migrationLink
        List<String> uris = Arrays.asList("/origin/data.json");
        when(mockDescription.getMigrationLink()).thenReturn("/destination");
        when(mockPage.getDescription()).thenReturn(mockDescription);
        when(mockContentReader.listUris()).thenReturn(uris);
        when(mockCollectionReader.getReviewed()).thenReturn(mockContentReader);
        when(mockContentReader.getContent("/origin")).thenReturn(mockPage);

        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        // When generateRedirectListForCollection is called
        redirectService.generateRedirectListForCollection(mockCollection, mockCollectionReader);

        // Then no CollectionRedirect should be added in the collection description 
        verify(mockCollectionDescription, times(0)).addRedirect(any());
    }

    @Test
    public void testGenerateRedirectListForCollectionNoLink() throws Exception {
        // Given a redirect API that doesn't have any redirects in it
        when(mockRedirectAPI.getRedirect(any())).thenThrow(new RedirectNotFoundException());

        // And a collection with a blank migrationLink
        List<String> uris = Arrays.asList("/origin/data.json");
        when(mockDescription.getMigrationLink()).thenReturn("");
        when(mockPage.getDescription()).thenReturn(mockDescription);
        when(mockContentReader.listUris()).thenReturn(uris);
        when(mockCollectionReader.getReviewed()).thenReturn(mockContentReader);
        when(mockContentReader.getContent("/origin")).thenReturn(mockPage);

        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        // When generateRedirectListForCollection is called
        redirectService.generateRedirectListForCollection(mockCollection, mockCollectionReader);

        // Then no CollectionRedirect should be added in the collection description 
        verify(mockCollectionDescription, times(0)).addRedirect(any());
    }

    @Test
    public void testGenerateRedirectListForCollectionDelete() throws Exception {
        // Given a redirect API that doesn't have any redirects in it
        when(mockRedirectAPI.getRedirect(any())).thenReturn(new Redirect("/origin", "/destination"));

        // And a collection with a blank migrationLink
        List<String> uris = Arrays.asList("/origin/data.json");
        when(mockDescription.getMigrationLink()).thenReturn("");
        when(mockPage.getDescription()).thenReturn(mockDescription);
        when(mockContentReader.listUris()).thenReturn(uris);
        when(mockCollectionReader.getReviewed()).thenReturn(mockContentReader);
        when(mockContentReader.getContent("/origin")).thenReturn(mockPage);

        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        // When generateRedirectListForCollection is called
        redirectService.generateRedirectListForCollection(mockCollection, mockCollectionReader);

        // Then the expected CollectionRedirect should be added in the collection description 
        CollectionRedirect expectedCollectionRedirect = new CollectionRedirect("/origin", "", CollectionRedirectAction.DELETE);
        verify(mockCollectionDescription, times(1)).addRedirect(expectedCollectionRedirect);
    }

    @Test
    public void testGenerateRedirectListForCollectionError() throws Exception {
        // Given a redirect API that doesn't have any redirects in it
        when(mockRedirectAPI.getRedirect(any())).thenThrow(new RedirectAPIException());

        // And a collection with a blank migrationLink
        List<String> uris = Arrays.asList("/origin/data.json");
        when(mockDescription.getMigrationLink()).thenReturn("");
        when(mockPage.getDescription()).thenReturn(mockDescription);
        when(mockContentReader.listUris()).thenReturn(uris);
        when(mockCollectionReader.getReviewed()).thenReturn(mockContentReader);
        when(mockContentReader.getContent("/origin")).thenReturn(mockPage);

        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        // When generateRedirectListForCollection is called an exception is thrown
        ZebedeeException ex = assertThrows(ZebedeeException.class, () -> redirectService.generateRedirectListForCollection(mockCollection, mockCollectionReader));

        // Then the message should show what has happened
        assertEquals("couldn't generate redirect from redirect API data", ex.getMessage());
    }

    @Test
    public void testGetCollectionRedirectUpdate() throws Exception {
        // Given the API returns a redirect for a key with a different to
        Redirect redirect = new Redirect("/from", "/to");
        Redirect apiRedirect = new Redirect("/from", "/totwo");

        when(mockRedirectAPI.getRedirect("/from")).thenReturn(apiRedirect);

        // When getCollectionRedirect is called
        CollectionRedirect collectionRedirect = redirectService.getCollectionRedirect(redirect);

        // Then the expected collectionRedirect should be returned
        assertEquals(redirect.getFrom(), collectionRedirect.getFrom());
        assertEquals(redirect.getTo(), collectionRedirect.getTo());
        assertEquals(CollectionRedirectAction.UPDATE, collectionRedirect.getAction());
    }

    @Test
    public void testGetCollectionRedirectDelete() throws Exception {
        // Given the collection has a blank migration path
        Redirect redirect = new Redirect("/from", "");
        // And the api has an existing redirect
        Redirect apiRedirect = new Redirect("/from", "/totwo");

        when(mockRedirectAPI.getRedirect("/from")).thenReturn(apiRedirect);
        RedirectService redirectService = new RedirectServiceImpl(mockRedirectAPI);

        // When getCollectionRedirect is called
        CollectionRedirect collectionRedirect = redirectService.getCollectionRedirect(redirect);

        // Then the expected collectionRedirect should be returned
        assertEquals(redirect.getFrom(), collectionRedirect.getFrom());
        assertEquals(redirect.getTo(), collectionRedirect.getTo());
        assertEquals(CollectionRedirectAction.DELETE, collectionRedirect.getAction());
    }

    @Test
    public void testGetCollectionRedirectNothing() throws Exception {
        // Given the collection has the same migration path
        Redirect redirect = new Redirect("/from", "/to");
        // And the api has an existing redirect that matches
        Redirect apiRedirect = new Redirect("/from", "/to");

        when(mockRedirectAPI.getRedirect("/from")).thenReturn(apiRedirect);

        // When getCollectionRedirect is called
        CollectionRedirect collectionRedirect = redirectService.getCollectionRedirect(redirect);

        // Then the expected collectionRedirect should be returned
        assertEquals(redirect.getFrom(), collectionRedirect.getFrom());
        assertEquals(redirect.getTo(), collectionRedirect.getTo());
        assertEquals(CollectionRedirectAction.NO_ACTION, collectionRedirect.getAction());
    }

    @Test
    public void testGetCollectionRedirectCreate() throws Exception {
        // Given the collection has the same migration path
        Redirect redirect = new Redirect("/from", "/to");
        // And the api has no existing redirect that matches
        when(mockRedirectAPI.getRedirect("/from")).thenThrow(new RedirectNotFoundException());

        // When getCollectionRedirect is called
        CollectionRedirect collectionRedirect = redirectService.getCollectionRedirect(redirect);

        // Then the expected collectionRedirect should be returned
        assertEquals(redirect.getFrom(), collectionRedirect.getFrom());
        assertEquals(redirect.getTo(), collectionRedirect.getTo());
        assertEquals(CollectionRedirectAction.CREATE, collectionRedirect.getAction());
    }

    @Test
    public void testGetCollectionRedirectNoActionBlank() throws Exception {
        // Given the collection has the same migration path
        Redirect redirect = new Redirect("/from", "");
        // And the api has no existing redirect that matches
        when(mockRedirectAPI.getRedirect("/from")).thenThrow(new RedirectNotFoundException());

        // When getCollectionRedirect is called
        CollectionRedirect collectionRedirect = redirectService.getCollectionRedirect(redirect);

        // Then the expected collectionRedirect should be returned
        assertEquals(redirect.getFrom(), collectionRedirect.getFrom());
        assertEquals(redirect.getTo(), collectionRedirect.getTo());
        assertEquals(CollectionRedirectAction.NO_ACTION, collectionRedirect.getAction());
    }

    @Test
    public void testPublishRedirectsCreate() throws Exception {
        List<CollectionRedirect> redirects = Arrays.asList(
                new CollectionRedirect("/old-site/bulletin-series/latest", "/new-site/bulletin-series", CollectionRedirectAction.CREATE),
                new CollectionRedirect("/old-site/bulletin-series/previousreleases", "/new-site/bulletin-series/previousreleases", CollectionRedirectAction.CREATE),
                new CollectionRedirect("/old-site/bulletin-series/latest/relateddata", "/new-site/bulletin-series/relateddata", CollectionRedirectAction.CREATE)
        );

        redirectService.publishRedirectsForCollection(redirects, TEST_COLLECTION_ID);

        verify(mockRedirectAPI, times(1)).putRedirect(argThat(r ->
                "/old-site/bulletin-series/latest".equals(r.getFrom()) &&
                        "/new-site/bulletin-series".equals(r.getTo())));
        verify(mockRedirectAPI, times(1)).putRedirect(argThat(r ->
                "/old-site/bulletin-series/previousreleases".equals(r.getFrom()) &&
                        "/new-site/bulletin-series/previousreleases".equals(r.getTo())));
        verify(mockRedirectAPI, times(1)).putRedirect(argThat(r ->
                "/old-site/bulletin-series/latest/relateddata".equals(r.getFrom()) &&
                        "/new-site/bulletin-series/relateddata".equals(r.getTo())));
        verifyNoMoreInteractions(mockRedirectAPI);
    }

    @Test
    public void testPublishRedirectsUpdate() throws Exception {
        List<CollectionRedirect> redirects = Collections.singletonList(
                new CollectionRedirect("/old-site/releases/my-release", "/new-site/releases/my-release2", CollectionRedirectAction.UPDATE)
        );

        redirectService.publishRedirectsForCollection(redirects, TEST_COLLECTION_ID);

        verify(mockRedirectAPI, times(1)).putRedirect(argThat(r ->
                "/old-site/releases/my-release".equals(r.getFrom()) &&
                        "/new-site/releases/my-release2".equals(r.getTo())));
        verifyNoMoreInteractions(mockRedirectAPI);
    }

    @Test
    public void testPublishRedirectsDelete204() throws Exception {
        List<CollectionRedirect> redirects = Collections.singletonList(
                new CollectionRedirect("/old-site/releases/my-release", "/new-site/releases/my-release", CollectionRedirectAction.DELETE)
        );

        // default mock: do nothing → simulate 204
        redirectService.publishRedirectsForCollection(redirects, TEST_COLLECTION_ID);

        verify(mockRedirectAPI, times(1)).deleteRedirect("/old-site/releases/my-release");
        verifyNoMoreInteractions(mockRedirectAPI);
    }

    @Test
    public void testPublishRedirectsDelete404Idempotent() throws Exception {
        List<CollectionRedirect> redirects = Collections.singletonList(
                new CollectionRedirect("/old-site/releases/missing", "/new-site/releases/missing", CollectionRedirectAction.DELETE)
        );

        doThrow(new RedirectAPIException("not found", 404))
                .when(mockRedirectAPI).deleteRedirect("/old-site/releases/missing");

        redirectService.publishRedirectsForCollection(redirects, TEST_COLLECTION_ID);

        verify(mockRedirectAPI, times(1)).deleteRedirect("/old-site/releases/missing");
        verifyNoMoreInteractions(mockRedirectAPI);
    }

    @Test
    public void testPublishRedirectsNoRedirects() throws Exception {
        redirectService.publishRedirectsForCollection(Collections.emptyList(), TEST_COLLECTION_ID);
        verifyNoInteractions(mockRedirectAPI);
    }

    @Test
    public void testPublishRedirectsNormalisesFrom() throws Exception {
        List<CollectionRedirect> redirects = Collections.singletonList(
                new CollectionRedirect("economy", "/business", CollectionRedirectAction.CREATE) // no leading slash
        );

        redirectService.publishRedirectsForCollection(redirects, TEST_COLLECTION_ID);

        verify(mockRedirectAPI).putRedirect(argThat(r ->
                "/economy".equals(r.getFrom()) && "/business".equals(r.getTo())));
        verifyNoMoreInteractions(mockRedirectAPI);
    }

    @Test
    public void testPublishRedirectsMixedFailuresAggregates() throws Exception {
        List<CollectionRedirect> redirects = Arrays.asList(
                new CollectionRedirect("/ok-create", "/to1", CollectionRedirectAction.CREATE),
                new CollectionRedirect("/fail-update", "/to2", CollectionRedirectAction.UPDATE),
                new CollectionRedirect("/fail-delete", "/to3", CollectionRedirectAction.DELETE)
        );

        // ok-create succeeds
        // update → 500
        doThrow(new RedirectAPIException("server err", 500))
                .when(mockRedirectAPI).putRedirect(argThat(r -> "/fail-update".equals(r.getFrom())));
        // delete → 400
        doThrow(new RedirectAPIException("bad request", 400))
                .when(mockRedirectAPI).deleteRedirect("/fail-delete");

        IOException ex = assertThrows(IOException.class,
                () -> redirectService.publishRedirectsForCollection(redirects, TEST_COLLECTION_ID));

        String msg = ex.getMessage();
        assertTrue(msg.contains("/fail-update"));
        assertTrue(msg.contains("/fail-delete"));

        verify(mockRedirectAPI).putRedirect(argThat(r -> "/ok-create".equals(r.getFrom())));
        verify(mockRedirectAPI).putRedirect(argThat(r -> "/fail-update".equals(r.getFrom())));
        verify(mockRedirectAPI).deleteRedirect("/fail-delete");
        verifyNoMoreInteractions(mockRedirectAPI);
    }

    @Test
    public void testPublishRedirectsNoActionIgnored() throws Exception {
        List<CollectionRedirect> redirects = Collections.singletonList(
                new CollectionRedirect("/x", "/y", CollectionRedirectAction.NO_ACTION)
        );

        redirectService.publishRedirectsForCollection(redirects, TEST_COLLECTION_ID);
        verifyNoInteractions(mockRedirectAPI);
    }
}

