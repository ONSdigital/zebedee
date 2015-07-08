package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.content.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigital.zebedee.Zebedee;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;



/**
 * Created by thomasridd on 06/07/15.
 */
public class ValidatorTest {
    public static final String ZEBEDEE_ROOT = "zebedee_root";

    static Zebedee zebedee;
    @BeforeClass
    public static void setup () {
        zebedee = new Zebedee(Paths.get(System.getenv(ZEBEDEE_ROOT)));

    }

    @Test
    public void basicTest() throws IOException {
        Validator validator = new Validator(zebedee);
//        String uri = "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/bulletins/cohortfertility/2013-12-05";
//        List<String> filesThatLinkToURI = validator.getFilesThatLinkToURI(uri);
//        System.out.println(filesThatLinkToURI.size() + " files found for " + uri);
//        for (String file: filesThatLinkToURI) {
//            System.out.println(file);
//        }

        validator.validate(zebedee.path.resolve("validator"));
    }

    //@Test
    public void testWrangler() throws IOException {
        Wrangler wrangler = new Wrangler(zebedee);
        wrangler.updateTimeSeriesNumbers();

        wrangler.updateTimeSeriesDetails(Paths.get("/Users/thomasridd/Documents/onswebsite/source/timeseriesdetails.csv"));
        wrangler.moveURIListFromCSV(Paths.get("/Users/thomasridd/Documents/onswebsite/source/moveuris.csv"));

    }

    @AfterClass
    public static void shutdown() {
        zebedee = null;
    }
}