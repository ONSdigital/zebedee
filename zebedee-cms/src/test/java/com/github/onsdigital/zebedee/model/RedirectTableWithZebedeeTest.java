package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.File;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 04/08/15.
 *
 * Motivation for the parent-child redirect is being able to
 */
public class RedirectTableWithZebedeeTest {
    Zebedee zebedee;
    Builder bob;
    String trueURI = "/themea/landinga/producta/bulletins/bulletina/2015-01-01";

    @Before
    public void setupTests() throws IOException {
        // Create a setup from
        bob = new Builder(RedirectTableWithZebedeeTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        zebedee = new Zebedee(bob.zebedee);
    }
    @After
    public void ripdownTests() {
        bob = null;
        zebedee = null;
    }

    @Test
    public void redirectTable_onZebedeeCreate_isCreated() {
        // Given
        // standard setup

        // When
        // we reference the redirect table
        RedirectTable redirect = zebedee.published.redirect;
        Path redirectPath = zebedee.published.path.resolve(Content.REDIRECT);

        // Then
        // table should be non null and exist
        assertNotNull(redirect);
        assertTrue(Files.exists(redirectPath));
    }

    @Test
    public void redirectTable_onCollectionCreate_areCreated() throws IOException {
        // Given
        // a collection setup
        CollectionDescription collectionDescription = new CollectionDescription("my collection");
        Collection collection = Collection.create(collectionDescription, zebedee, bob.publisher1.email);

        // When
        // we reference the redirect tables
        RedirectTable redirectTable = collection.redirect;
        RedirectTable inProgressRedirect = collection.inProgress.redirect;
        RedirectTable completeRedirect = collection.complete.redirect;
        RedirectTable reviewedRedirect = collection.reviewed.redirect;

        // Then
        // we expect the tables to be created
        assertNotNull(redirectTable);
        assertNotNull(inProgressRedirect);
        assertTrue(Files.exists(collection.inProgress.path.resolve(Content.REDIRECT)));
        assertNotNull(completeRedirect);
        assertTrue(Files.exists(collection.complete.path.resolve(Content.REDIRECT)));
        assertNotNull(reviewedRedirect);
        assertTrue(Files.exists(collection.reviewed.path.resolve(Content.REDIRECT)));
    }

    @Test
    public void publishedRedirectTable_withZebedeePublishedRedirect_doesRedirect() {
        // Given
        // standard setup with a redirect in the published table
        String falseURI = "/themea/landinga/producta/bulletins/falsebulletin/2015-01-01";
        zebedee.published.redirect.addRedirect(falseURI, trueURI);

        // When
        // we
        Path path = zebedee.published.get(falseURI);
        String uri = zebedee.toUri(path);

        // Then
        // we should get
        assertEquals(trueURI, uri);
    }

    @Test
    public void publishedRedirectTable_withZebedeeCollectionRedirect_doesRedirect() throws IOException {
        // Given
        // standard setup with a redirect in the published table plus a collection
        String falseURI = "/themea/landinga/producta/bulletins/falsebulletin/2015-01-01";
        zebedee.published.redirect.addRedirect(falseURI, trueURI);

        CollectionDescription collectionDescription = new CollectionDescription("my collection");
        Collection collection = Collection.create(collectionDescription, zebedee, bob.publisher1.email);

        // When
        // we search for the false URI
        Path path = collection.find(bob.publisher1.email, falseURI);
        String uri = zebedee.toUri(path);

        // Then
        // the collection parent-child redirect ought to handle the redirect
        assertEquals(trueURI, uri);
    }

    @Test
    public void collectionRedirectTable_withZebedeeCollectionRedirect_doesRedirect() throws IOException {
        // Given
        // standard setup with a redirect in the published table plus a collection
        String falseURI = "/themea/landinga/producta/bulletins/falsebulletin/2015-01-01";


        CollectionDescription collectionDescription = new CollectionDescription("my collection");
        Collection collection = Collection.create(collectionDescription, zebedee, bob.publisher1.email);
        collection.inProgress.redirect.addRedirect(falseURI, trueURI);

        // When
        // we search for the false URI
        Path path = collection.find(bob.publisher1.email, falseURI);
        String uri = zebedee.toUri(path);

        // Then
        // the collection parent-child redirect ought to handle the redirect
        assertEquals(trueURI, uri);
    }
}