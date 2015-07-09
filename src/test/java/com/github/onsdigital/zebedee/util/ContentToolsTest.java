package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.Zebedee;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;


/**
 * Created by thomasridd on 06/07/15.
 */
public class ContentToolsTest {
    public static final String ZEBEDEE_ROOT = "zebedee_root";

    static Zebedee zebedee;
    @BeforeClass
    public static void setup () {
        zebedee = new Zebedee(Paths.get(System.getenv(ZEBEDEE_ROOT)));

    }

    @Test
    public void testTheLibrarian() throws IOException {

        Librarian librarian = new Librarian(zebedee);

        librarian.catalogue();

        assertNotEquals(0, librarian.articles.size());
        assertNotEquals(0, librarian.bulletins.size());
        assertNotEquals(0, librarian.datasets.size());
        assertNotEquals(0, librarian.pages.size());
    }

    @Test
    public void testPenelopeWillKnit() {
        Penelope penelope = new Penelope(zebedee);
        penelope.knit();
    }

    //@Test
    public void testWrangler() throws IOException {
        Wrangler wrangler = new Wrangler(zebedee);
        wrangler.updateTimeSeriesNumbers();
        wrangler.updateTimeSeriesDetails(Paths.get("/Users/thomasridd/Documents/onswebsite/source/timeseriesdetails.csv"));

    }

    @AfterClass
    public static void shutdown() {
        zebedee = null;
    }
}