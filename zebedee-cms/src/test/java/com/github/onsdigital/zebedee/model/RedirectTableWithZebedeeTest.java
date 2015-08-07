package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by thomasridd on 04/08/15.
 *
 * Motivation for the parent-child redirect is being able to
 */
public class RedirectTableWithZebedeeTest {
    Zebedee zebedee;
    Builder bob;

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

    //------------------------------------------------------
    //
    // Different content for in parent-child
    //
    // Parent-child redirect can be used in Zebedee with a parent child
    // chain of inProgress, Complete, Reviewed, Published
    //
    // Given - child (the published
    // When - we redirect
    // Then - we expect the combination to process redirects with moves made in collections taking priority

}