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

import com.github.onsdigital.zebedee.util.slack.Notifier;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;

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

    @Mock
    private Notifier mockNotifier;

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
    public void testPublishRedirectsForCollectionValidCreate() throws Exception {
        CollectionRedirect redirect = new CollectionRedirect("/from", "/to", CollectionRedirectAction.CREATE);
        when(mockRedirectAPI.getRedirect("/from")).thenThrow(new RedirectNotFoundException());
        when(mockCollection.getId()).thenReturn(TEST_COLLECTION_ID);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockCollectionDescription.getRedirects()).thenReturn(Collections.singletonList(redirect));

        redirectService.publishRedirectsForCollection(mockCollection, mockNotifier);

        ArgumentCaptor<Redirect> redirectCaptor = ArgumentCaptor.forClass(Redirect.class);
        verify(mockRedirectAPI).putRedirect(redirectCaptor.capture());

        Redirect actualRedirect = redirectCaptor.getValue();
        assertEquals("/from", actualRedirect.getFrom());
        assertEquals("/to", actualRedirect.getTo());
    }

    @Test
    public void testPublishRedirectsForCollectionInvalidCreate() throws Exception {
        CollectionRedirect redirect = new CollectionRedirect("/exists", "/to", CollectionRedirectAction.CREATE);
        when(mockRedirectAPI.getRedirect("/exists")).thenReturn(new Redirect("/exists", "/to"));
        when(mockCollection.getId()).thenReturn(TEST_COLLECTION_ID);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockCollectionDescription.getRedirects()).thenReturn(Collections.singletonList(redirect));

        redirectService.publishRedirectsForCollection(mockCollection, mockNotifier);

        verify(mockRedirectAPI, never()).putRedirect(any());
        verify(mockNotifier, times(1)).sendCollectionWarning(eq(mockCollection), anyString(), contains("skipped"));
    }

    @Test
    public void testPublishRedirectsForCollectionDelete404Ignored() throws Exception {
        CollectionRedirect redirect = new CollectionRedirect("/delete-me", "", CollectionRedirectAction.DELETE);
        when(mockRedirectAPI.getRedirect("/delete-me")).thenReturn(new Redirect("/delete-me", "/something"));
        doThrow(new RedirectAPIException("Not Found", 404)).when(mockRedirectAPI).deleteRedirect("/delete-me");
        when(mockCollection.getId()).thenReturn(TEST_COLLECTION_ID);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockCollectionDescription.getRedirects()).thenReturn(Collections.singletonList(redirect));

        redirectService.publishRedirectsForCollection(mockCollection, mockNotifier);

        verify(mockRedirectAPI).deleteRedirect("/delete-me");
        verify(mockNotifier, never()).sendCollectionWarning(any(), anyString(), anyString());
    }

    @Test
    public void testPublishRedirectsForCollectionFailsWithIOException() throws Exception {
        CollectionRedirect redirect = new CollectionRedirect("/fail", "/to", CollectionRedirectAction.UPDATE);
        when(mockRedirectAPI.getRedirect("/fail")).thenReturn(new Redirect("/fail", "/old"));
        doThrow(new RedirectAPIException("Internal Server Error", 500)).when(mockRedirectAPI).putRedirect(any());
        when(mockCollection.getId()).thenReturn(TEST_COLLECTION_ID);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockCollectionDescription.getRedirects()).thenReturn(Collections.singletonList(redirect));

        IOException ex = assertThrows(IOException.class, () -> {
            redirectService.publishRedirectsForCollection(mockCollection, mockNotifier);
        });

        assertTrue(ex.getMessage().contains("Redirect operations failed"));
    }

}

