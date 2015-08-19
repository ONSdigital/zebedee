package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 17/08/15.
 */
public class RedirectTablePartialMatchTest {
    Zebedee zebedee = null;
    Path root = null;

    @Before
    public void setUp() throws Exception {
        // Create a
        Builder bob = new Builder(RedirectTablePartialMatchTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        root = bob.zebedee;
        zebedee = new Zebedee(root);
    }

    @After
    public void tearDown() throws Exception {
        // Garbage collection
        zebedee = null;
        FileUtils.deleteDirectory(root.toFile());
    }

    @Test
    public void redirectTable_whenSetup_shouldNotBeNull() {
        // Given
        // Content to set up the redirect
        RedirectTable table = new RedirectTablePartialMatch(zebedee.published);

        // When
        // We initialise a redirect

        // Then
        // It should not be null
        assertNotNull(table);
    }

    @Test
    public void get_forExistingContent_shouldReturnUri() {
        // Given
        // a table with a redirect
        RedirectTable table = new RedirectTablePartialMatch(zebedee.published);

        // When
        // We get the redirect
        String direct = table.get("themea/data.json");

        // Then
        // It should not be null
        assertNotNull(direct);
        assertEquals("themea/data.json", direct);
    }

    @Test
    public void get_forRedirectWhereContentExists_shouldReturnExistingUri() {
        // Given
        // a table with a redirect from existing data
        RedirectTable table = new RedirectTablePartialMatch(zebedee.published);
        table.addRedirect("themea/data.json", "themeb/data.json");

        // When
        // We get the redirect
        String redirect = table.get("themea/data.json");

        // Then
        // We expect the get to return the existing data
        assertNotNull(redirect);
        assertEquals("themea/data.json", redirect);
    }

    @Test
    public void get_whereRedirectExistsToContent_shouldRedirect() {
        // Given
        // a table with a redirect to real content
        RedirectTable table = new RedirectTablePartialMatch(zebedee.published);
        table.addRedirect("redirect/data.json", "themea/data.json");

        // When
        // We get the redirect
        String redirected = table.get("redirect/data.json");

        // Then
        // It should not be null
        assertNotNull(redirected);
        assertEquals("themea/data.json", redirected);
    }

    @Test
    public void get_whereRedirectContentDoesntExist_shouldReturnNull() {
        // Given
        // a table with a redirect to something that doesn't exist
        RedirectTable table = new RedirectTablePartialMatch(zebedee.published);
        table.addRedirect("redirect/data.json", "does/not/exist/data.json");

        // When
        // We get the redirect
        String redirected = table.get("redirect/data.json");

        // Then
        // It should be null
        assertNull(redirected);
    }

    @Test
    public void get_whereMultipleRedirectsExistFromPartialMatchOrigin_shouldRedirect() {
        // Given
        // a quite complicated situation...
        RedirectTable table = new RedirectTablePartialMatch(zebedee.published);

        // Take a top level node and move it
        table.addRedirect("business", "themea");
        // Recreate the top level node (not necessary in test) and move that
        table.addRedirect("business", "themeb");

        // When
        // We get the redirects from two node that have moved
        String pageThatBelongedToTheOriginalBusiness = table.get("business/landinga/data.json");
        String pageThatBelongedToTheSecondBusiness = table.get("business/landingc/data.json");

        // Then
        // The appropriate links work even though they are to different places
        assertEquals("themea/landinga/data.json", pageThatBelongedToTheOriginalBusiness);
        assertEquals("themeb/landingc/data.json", pageThatBelongedToTheSecondBusiness);

    }
}