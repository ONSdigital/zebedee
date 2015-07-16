package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 14/07/15.
 */
public class UtilsTest {

    @Test
    public void testTheLibrarian() throws IOException {
        // With a basic zebedee setup
        Builder bob = new Builder(ContentToolsTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we run catalogue
        Librarian librarian = new Librarian(zebedee);
        librarian.catalogue();

        // Check the librarian has picked up 1 article, and 1 bulletin
        assertEquals(1, librarian.articles.size());
        assertEquals(1, librarian.bulletins.size());
    }

    @Test
    public void testIntegrityCheckDoesntPickUpGoodLinks() throws IOException {
        // With a basic zebedee setup
        Builder bob = new Builder(ContentToolsTest.class, ResourceUtils.getPath("/bootstraps/basic"));
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
    public void testIntegrityCheckDetectsBrokenLinks() throws IOException {
        // With a basic zebedee setup
        Builder bob = new Builder(ContentToolsTest.class, ResourceUtils.getPath("/bootstraps/broken"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we run integrity check
        Librarian librarian = new Librarian(zebedee);
        librarian.catalogue();
        librarian.checkIntegrity();

        // Check the librarian has picked up broken links
        assertTrue(librarian.contentErrors.size() > 0);
    }

    @Test
    public void testJsonValidityDoesntPickupGoodJSON() throws IOException {
        // With a basic zebedee setup
        Builder bob = new Builder(ContentToolsTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we validate all JSON
        Librarian librarian = new Librarian(zebedee);
        librarian.validateJSON();

        // Check the librarian does not pick up any of these files
        assertTrue(librarian.invalidJson.size() == 0);
    }

    @Test
    public void testJsonValidityDoesPickupBadJSON() throws IOException {
        // With a basic zebedee setup
        Builder bob = new Builder(ContentToolsTest.class, ResourceUtils.getPath("/bootstraps/broken"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we validate all JSON
        Librarian librarian = new Librarian(zebedee);
        librarian.validateJSON();

        // Check the librarian does pick up
        assertTrue(librarian.invalidJson.size() > 0);
    }
}