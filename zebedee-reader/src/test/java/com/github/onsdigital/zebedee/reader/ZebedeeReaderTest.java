package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.dynamic.DescriptionWrapper;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Point;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Series;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.StaticPage;
import com.github.onsdigital.zebedee.content.page.statistics.document.article.Article;
import com.github.onsdigital.zebedee.content.page.taxonomy.TaxonomyLandingPage;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by bren on 31/07/15.
 */

public class ZebedeeReaderTest {
    private final static String TEST_COLLECTION_ID = "testcollection-testid";
    private final static String TEST_SESSION_ID = "testcollection-session";


    //TODO: mime type resolving not working on Mac machines due to java bug in Files.probeContentType, use a lib to resolve mime type based on extension and enable test bits checking mime types

    static {
        ReaderConfiguration.init("target/test-classes/test-content/");

        if (ZebedeeReader.getCollectionReaderFactory() == null) {
            ZebedeeReader.setCollectionReaderFactory(new FakeCollectionReaderFactory(ReaderConfiguration.getConfiguration().getCollectionsFolder()));
        }
    }

    @Test
    public void testReadPublishedContent() throws ZebedeeException, IOException {
        readAccessibilityData("about/accessibility///");
    }


    @Test
    public void testReadWelshContent() throws ZebedeeException, IOException {
        Content content = createReader(ContentLanguage.cy).getPublishedContent("peoplepopulationandcommunity");
        assertTrue(content instanceof TaxonomyLandingPage);
        TaxonomyLandingPage landingPage = (TaxonomyLandingPage) content;
        assertEquals("Pobl, poblogaeth a chymuned", landingPage.getDescription().getTitle());
    }

    @Test
    public void testGetChildrenInWelsh() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = createReader(ContentLanguage.cy).getPublishedContentChildren("peoplepopulationandcommunity/culturalidentity/ethnicity/articles/ethnicityandthelabourmarket2011censusenglandandwales");
        URI articleUri = URI.create("/peoplepopulationandcommunity/culturalidentity/ethnicity/articles/ethnicityandthelabourmarket2011censusenglandandwales/2014-11-13");
        assertTrue(children.containsKey(articleUri));
        ContentNode article = children.get(articleUri);
        assertEquals("Ethnigrwydd a'r Farchnad Lafur, Cyfrifiad 2011, Cymru a Lloegr", article.getDescription().getTitle());
    }

    @Test
    public void testReadPublishedContentWithAbsoluteUri() throws ZebedeeException, IOException {
        readAccessibilityData("/about/accessibility///");
    }

    @Test
    public void testTitleFilter() throws ZebedeeException, IOException {
        Content content = createReader().getPublishedContent("about/accessibility", new DataFilter(DataFilter.FilterType.TITLE));
        assertNotNull(content);
        assertTrue(content instanceof ContentNodeDetails);
        ContentNodeDetails titleWrapper = (ContentNodeDetails) content;
        assertNotNull(titleWrapper);
        assertEquals("Accessibility", titleWrapper.getTitle());
    }

    @Test
    public void testDescriptionFilter() throws ZebedeeException, IOException {
        Content content = createReader().getCollectionContent(TEST_COLLECTION_ID, TEST_SESSION_ID, "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02", new DataFilter(DataFilter.FilterType.DESCRIPTION));
        assertNotNull(content);
        assertTrue(content instanceof DescriptionWrapper);
        DescriptionWrapper description = (DescriptionWrapper) content;
        assertEquals("Labour disputes by sector: LABD02", description.getDescription().getTitle());
        assertEquals("Richard Clegg", description.getDescription().getContact().getName());
    }

    @Test
    public void testSeriesFilter() throws ZebedeeException, IOException {
        HashMap<String, String[]> parameters = new HashMap<>();
        parameters.put("frequency", new String[]{"quarters"});
        parameters.put("fromYear", new String[]{"2001"});
        parameters.put("fromQuarter", new String[]{"Q2"});

        parameters.put("toYear", new String[]{"2005"});
        parameters.put("toQuarter", new String[]{"q1"});

        Content content = createReader().getPublishedContent("/employmentandlabourmarket/peopleinwork/earningsandworkinghours/timeseries/a2f8/", new DataFilter(DataFilter.FilterType.SERIES, parameters));
        assertNotNull(content);
        assertTrue(content instanceof Series);
        Series series = (Series) content;
        Point next = series.getSeries().iterator().next();
        assertEquals("2001 Q2", next.getName());
        assertEquals(16, series.getSeries().size());
    }


    @Test(expected = NotFoundException.class)
    public void testNonExistingContentRead() throws ZebedeeException, IOException {
        createReader().getPublishedContent("non/existing/path/");
    }

    @Test
    public void testReadPublishedResource() throws ZebedeeException, IOException {
        try (Resource resource = createReader().getPublishedResource("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/4f5b14cb.xls")) {
            assertNotNull(resource != null);
//            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    @Test
    public void testReadCollectionContent() throws ZebedeeException, IOException {
        Content content = createReader().getCollectionContent(TEST_COLLECTION_ID, TEST_SESSION_ID, "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16");
        assertNotNull(content);
        assertTrue(content instanceof Page);
        Page page = (Page) content;
        assertEquals(page.getType(), PageType.article);
        assertTrue(content instanceof Article);
    }

    @Test(expected = CollectionNotFoundException.class)
    public void testNonExistingCollectionRead() throws ZebedeeException, IOException {
        Content content = createReader().getCollectionContent("nonexistingcollection", TEST_SESSION_ID, "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/0c908062.json");
    }

    @Test
    public void testXlsResource() throws ZebedeeException, IOException {
        try (Resource resource = createReader().getCollectionResource(TEST_COLLECTION_ID, TEST_SESSION_ID, "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02/labd02jul2015_tcm77-408195.xls")) {
            assertNotNull(resource != null);
//            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    @Test
    public void testGetChildren() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = createReader().getPublishedContentChildren("peoplepopulationandcommunity/culturalidentity/ethnicity");
        Map.Entry<URI, ContentNode> contentNode = children.entrySet().iterator().next();
        assertTrue(children.containsKey(URI.create("/peoplepopulationandcommunity/culturalidentity/ethnicity/articles")));
        URI bulletinUri = URI.create("/peoplepopulationandcommunity/culturalidentity/ethnicity/bulletins");
        assertTrue(children.containsKey(bulletinUri));
        assertNull(contentNode.getValue().getType());//type is null for directories with no data.json
        assertNull(contentNode.getValue().getChildren());// only immediate children should be read
        assertEquals("bulletins", children.get(bulletinUri).getDescription().getTitle());
    }

    @Test
    public void testGetCollectionChildren() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = createReader().getCollectionContentChildren(TEST_COLLECTION_ID, TEST_SESSION_ID, "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions");
        URI datasetsUri = URI.create("/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets");
        assertTrue(children.containsKey(datasetsUri));
        URI articlesUri = URI.create("/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles");
        assertTrue(children.containsKey(articlesUri));
        assertNull(children.get(articlesUri).getChildren());
        assertNull(children.get(datasetsUri).getChildren());
    }

    private Page readAccessibilityData(String path) throws ZebedeeException, IOException {
        Content content = readPublishedContent(path);
        assertTrue(content instanceof Page);
        Page page = (Page) content;
        assertEquals(page.getType(), PageType.static_page);
        assertEquals("Accessibility", page.getDescription().getTitle());
        assertTrue(page instanceof StaticPage);
        StaticPage staticPage = (StaticPage) content;
        assertNotNull(staticPage.getMarkdown());
        return page;
    }

    private Content readPublishedContent(String path) throws ZebedeeException, IOException {
        Content content = createReader().getPublishedContent(path);
        assertNotNull(content);
        return content;
    }

    private ZebedeeReader createReader() {
        return createReader(null);
    }

    private ZebedeeReader createReader(ContentLanguage language) {
        return new ZebedeeReader(language);
    }


}


