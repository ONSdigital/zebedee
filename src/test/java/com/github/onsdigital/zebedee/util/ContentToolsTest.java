package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;


/**
 * Created by thomasridd on 06/07/15.
 */
public class ContentToolsTest {
    public static final String ZEBEDEE_ROOT = "zebedee_root";

    @BeforeClass
    public static void setup () throws IOException {

    }

    @Test
    public void testTheLibrarian() throws IOException {
        // With a basic zebedee setup
        Builder bob = new Builder(ContentToolsTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        Zebedee zebedee = new Zebedee(bob.zebedee);

        // When we run catalogue
        Yaffle yaffle = new Yaffle(zebedee);
        yaffle.catalogue();

        // Check the librarian has picked up 1 article, and 1 bulletin
        assertEquals(1, yaffle.articles.size());
        assertEquals(1, yaffle.bulletins.size());
    }


    @AfterClass
    public static void shutdown() {

    }
}