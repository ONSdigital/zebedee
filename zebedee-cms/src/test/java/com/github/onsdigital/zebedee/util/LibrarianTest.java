package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by thomasridd on 14/07/15.
 */
public class LibrarianTest {

    @Test
    public void catalogue_givenData_returnsContent() throws Exception {
        // With a basic zebedee setup
        Builder bob = new Builder(LibrarianTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we run catalogue
        Librarian librarian = new Librarian(zebedee);
        librarian.catalogue();

        // Check the librarian has picked up 1 article, and 1 bulletin
        assertEquals(1, librarian.articles.size());
        assertEquals(1, librarian.bulletins.size());
    }

    @Test
    public void testIntegrity_givenGoodData_doesntReturnErrors() throws Exception {
        // With a basic zebedee setup
        Builder bob = new Builder(LibrarianTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we run catalogue
        Librarian librarian = new Librarian(zebedee);
        librarian.catalogue();
        librarian.checkIntegrity();

        // Check the librarian has checked items and not found anything
        assertTrue(librarian.checkedUris > 0);
        assertEquals(0, librarian.contentErrors.size());
    }

    @Test
    public void testIntegrity_givenBrokenLinks_returnsErrors() throws Exception {
        // With a basic zebedee setup
        Builder bob = new Builder(LibrarianTest.class, ResourceUtils.getPath("/bootstraps/broken"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we run integrity check
        Librarian librarian = new Librarian(zebedee);
        librarian.catalogue();
        librarian.checkIntegrity();

        // Check the librarian has picked up broken links
        assertTrue(librarian.contentErrors.size() > 0);
    }

    @Test
    public void jsonValidity_givenJSONFiles_DoesntPickupValidJSON() throws Exception {
        // With a basic zebedee setup
        Builder bob = new Builder(LibrarianTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we validate all JSON
        Librarian librarian = new Librarian(zebedee);
        librarian.validateJSON();

        // Check the librarian does not pick up any of these files
        assertTrue(librarian.invalidJson.size() == 0);
    }

    @Test
    public void jsonValidity_givenJSONFiles_DoesPickupInvalidJSON() throws Exception {
        // With a basic zebedee setup
        Builder bob = new Builder(LibrarianTest.class, ResourceUtils.getPath("/bootstraps/broken"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we validate all JSON
        Librarian librarian = new Librarian(zebedee);
        librarian.validateJSON();

        // Check the librarian does pick up
        assertTrue(librarian.invalidJson.size() > 0);
    }


    @Test
    public void contentUtil_givenBrokenLinks_returnsValidResults() throws Exception {
        // With
        //
        Builder bob = new Builder(LibrarianTest.class, ResourceUtils.getPath("/bootstraps/broken"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we validate all JSON
        Librarian librarian = new Librarian(zebedee);
        librarian.checkResolvable();

        // Check the librarian doesn't pickup any of these files
        assertTrue(librarian.unresolvableContent.size() == 0);
    }

}