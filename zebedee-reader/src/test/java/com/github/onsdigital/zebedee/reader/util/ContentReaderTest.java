package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.staticpage.StaticPage;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.configuration.TestConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by bren on 30/07/15.
 */

/*Notice that resources must be generated for these tests to pass. Maven test phase runs after resources are generated.
* If you want to run the tes in your ide make sure
* -Resources are generated ( maven generate-resources )
* -Run configuration points to zebedee-reader module root as it is default in maven and most ides (intellij seems to be not doing this)
* */

public class ContentReaderTest {

    private ContentReader contentReader;

    @Before
    public void createContentReader() {
        this.contentReader = new ContentReader(TestConfiguration.getTestZebedeeRoot());
    }

    @Test
    public void testGetAvailableContent() throws ZebedeeException, IOException {
        Content content = contentReader.getContent("master/about/accessibility/data.json///");
        assertNotNull(content);
        assertEquals(content.getType(), ContentType.static_page);
        assertEquals("Accessibility",  content.getDescription().getTitle());
        assertTrue(content instanceof StaticPage);
        StaticPage staticPage = (StaticPage) content;
        assertNotNull(staticPage.getMarkdown());
    }

    @Test(expected = NotFoundException.class)
    public void testGetNonexistingContent() throws ZebedeeException, IOException {
        Content content = contentReader.getContent("master/madeupfoldername/data.json");
    }

    @Test(expected = BadRequestException.class)
    public void testReadDirectoryAsContent() throws ZebedeeException, IOException {
        Content content = contentReader.getContent("master/about/accessibility////");
    }

    @Test(expected = BadRequestException.class)
    public void testStartingWithForwardSlash() throws ZebedeeException, IOException {
        Content content = contentReader.getContent("/master/madeupfoldername/data.json");
    }

    @Test
    public void testXlsResource() throws ZebedeeException, IOException {
        try (Resource resource = contentReader.getResource("master/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/4f5b14cb.xls")) {
            assertNotNull(resource);
            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    @Test
    public void testPngResource() throws ZebedeeException, IOException {
        try (Resource resource = contentReader.getResource("master/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2015-07-09/5afe3d27-download.png")) {
            assertNotNull(resource != null);
            assertEquals("image/png", resource.getMimeType());
            assertTrue(resource.getData().available() > 0);
        }
    }

    @Test
    public void testHtmlResource() throws ZebedeeException, IOException {
        try (Resource resource = contentReader.getResource("master/peoplepopulationandcommunity/culturalidentity/ethnicity/articles/ethnicityandthelabourmarket2011censusenglandandwales/2014-11-13/19df5bcf.html")) {
            assertNotNull(resource != null);
            assertEquals("text/html", resource.getMimeType());
            assertTrue(resource.getData().available() > 0);
        }
    }


}
