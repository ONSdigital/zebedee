package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.StaticPage;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart.Chart;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
        this.contentReader = new FileSystemContentReader("target/test-classes/test-content/zebedee/master");
    }

    @Test
    public void testGetAvailableContent() throws ZebedeeException, IOException {
        Page content = contentReader.getContent("about/accessibility");
        assertNotNull(content);
        assertEquals(content.getType(), PageType.static_page);
        assertEquals("Accessibility", content.getDescription().getTitle());
        assertTrue(content instanceof StaticPage);
        StaticPage staticPage = (StaticPage) content;
        assertNotNull(staticPage.getMarkdown());
    }

    @Test
    public void testGetHome() throws ZebedeeException, IOException {
        Page content = contentReader.getContent("/");
        assertNotNull(content);
        assertEquals(content.getType(), PageType.home_page);
        assertEquals("Home", content.getDescription().getTitle());
    }

    @Test
    public void testGetChart() throws ZebedeeException, IOException {
        Page content = contentReader.getContent("economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2015-07-09/0b6d65e2");
        assertTrue(content instanceof Chart);
    }

    @Test
    public void testGetTable() throws ZebedeeException, IOException {
        Page content = contentReader.getContent("economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/4f5b14cb");
        assertTrue(content instanceof Table);
    }

    @Test(expected = NotFoundException.class)
    public void testGetNonexistingContent() throws ZebedeeException, IOException {
        Page content = contentReader.getContent("madeupfoldername");
    }

    @Test(expected = NotFoundException.class)
    public void testReadDataWithNoDataFile() throws ZebedeeException, IOException {
        Page content = contentReader.getContent("about/testfolder////");
    }

    @Test
    public void testStartingWithForwardSlash() throws ZebedeeException, IOException {
        Page content = contentReader.getContent("/about/accessibility");
    }

    @Test
    public void testXlsResource() throws ZebedeeException, IOException {
        try (Resource resource = contentReader.getResource("economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/4f5b14cb.xls")) {
            assertNotNull(resource);
//            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    @Test
    public void testPngResource() throws ZebedeeException, IOException {
        try (Resource resource = contentReader.getResource("economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2015-07-09/5afe3d27-download.png")) {
            assertNotNull(resource != null);
//            assertEquals("image/png", resource.getMimeType());
            assertTrue(resource.getData().available() > 0);
        }
    }

    @Test
    public void testHtmlResource() throws ZebedeeException, IOException {
        try (Resource resource = contentReader.getResource("peoplepopulationandcommunity/culturalidentity/ethnicity/articles/ethnicityandthelabourmarket2011censusenglandandwales/2014-11-13/19df5bcf.html")) {
            assertNotNull(resource != null);
//            assertEquals("text/html", resource.getMimeType());
            assertTrue(resource.getData().available() > 0);
        }
    }


    @Test
    public void testGetChildrenDirectories() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = contentReader.getChildren("peoplepopulationandcommunity/culturalidentity/ethnicity");
        assertTrue(children.size() == 2);
        Map.Entry<URI, ContentNode> entry = children.entrySet().iterator().next();
        URI articleUri = URI.create("/peoplepopulationandcommunity/culturalidentity/ethnicity/articles");
        assertTrue(children.containsKey(articleUri));
        String bulletinUri = "/peoplepopulationandcommunity/culturalidentity/ethnicity/bulletins";
        assertTrue(children.containsKey(URI.create(bulletinUri)));
        assertNull(entry.getValue().getType());//type is null for directories with no data.json
        assertEquals("articles", children.get(articleUri).getDescription().getTitle());
    }


    @Test(expected = NotFoundException.class)
    public void testNonExistingNodeChilren() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = contentReader.getChildren("/nonexistingpath/test");
    }

    @Test
    public void testGetParents() throws ZebedeeException, IOException {
        //note that culturalidentity folder does not have data.json in test content, so it should be skipped
        Map<URI, ContentNode> parents = contentReader.getParents("peoplepopulationandcommunity/culturalidentity/ethnicity");
        assertTrue(parents.size() == 2);
        assertTrue(parents.containsKey(URI.create("/")));
        assertTrue(parents.containsKey(URI.create("/peoplepopulationandcommunity")));
    }

    @Test
    public void testGetParentsInWelsh() throws ZebedeeException, IOException {
        contentReader.setLanguage(ContentLanguage.cy);
        //note that culturalidentity folder does not have data.json in test content, so it should be skipped
        Map<URI, ContentNode> parents = contentReader.getParents("peoplepopulationandcommunity/culturalidentity/ethnicity");
        assertTrue(parents.containsKey(URI.create("/")));
        URI peoplePopulation = URI.create("/peoplepopulationandcommunity");
        assertTrue(parents.containsKey(peoplePopulation));
        assertEquals(parents.get(peoplePopulation).getDescription().getTitle(), "Pobl, poblogaeth a chymuned");
    }

    @Test
    public void testGetChildrenContent() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = contentReader.getChildren("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk");
        assertTrue(children.size() == 1);
        Map.Entry<URI, ContentNode> contentNode = children.entrySet().iterator().next();
        assertEquals("UK Natural Capital Land Cover in the UK", contentNode.getValue().getDescription().getTitle());
//        assertEquals(PageType.article, contentNode.getValue().getType());
        assertEquals("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17", contentNode.getKey().toString());
    }

    @Test
    public void testGetHomeChildren() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = contentReader.getChildren("/");
        assertTrue(children.containsKey(URI.create("/economy")));
        assertTrue(children.containsKey(URI.create("/about")));
    }

    @Test
    public void testGetLatestContent() throws ZebedeeException, IOException {
        Page latestContent = contentReader.getLatestContent("/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts");
        assertEquals("2015", latestContent.getDescription().getEdition());
    }

    @Test
    public void testGetContentLength() throws ZebedeeException, IOException {
        long contentLength = contentReader.getContentLength("economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/4f5b14cb.xls");
        assertEquals(56320, contentLength);
    }

    @Test
    public void listTimeseries_whereWeHaveTimeSeries_returnsTimeseriesDirectories() throws IOException {
        // Given
        //
        ContentReader reader = new FileSystemContentReader(this.contentReader.getRootFolder().resolve("employmentandlabourmarket").toString());

        // When
        //
        List<Path> timeSeriesDirectories = reader.listTimeSeriesDirectories();

        // Then
        //
        assertEquals(1, timeSeriesDirectories.size());
        assertTrue(timeSeriesDirectories.get(0).endsWith("timeseries"));
    }
//    @Test
//    public void mimeTypeShouldNotBeEmptyForXls() throws IOException {
//        String mimeType = ContentReader.determineMimeType(Paths.get("/some/path/data.xls"));
//        assertNotNull(mimeType);
//    }
//
//    @Test
//    public void mimeTypeShouldNotBeEmptyForXlsx() throws IOException {
//        String mimeType = ContentReader.determineMimeType(Paths.get("/some/path/data.xlsx"));
//        assertNotNull(mimeType);
//    }
}
