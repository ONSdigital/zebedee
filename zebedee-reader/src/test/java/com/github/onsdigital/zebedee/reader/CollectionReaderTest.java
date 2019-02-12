package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.statistics.document.article.Article;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by bren on 30/07/15.
 */

/*Notice that resources must be generated for these tests to pass. Maven test phase runs after resources are generated.* */

public class CollectionReaderTest {

    private final static String COLLECTION_ID = "testcollection-testid";

    static {
        ReaderConfiguration.init("target/test-classes/test-content/");

        if (ZebedeeReader.getCollectionReaderFactory() == null) {
            ZebedeeReader.setCollectionReaderFactory(new FakeCollectionReaderFactory(ReaderConfiguration.getConfiguration().getCollectionsFolder()));
        }
    }

    private CollectionReader collectionReader;

    @Before
    public void createContentReader() throws IOException, NotFoundException {
        this.collectionReader = new FakeCollectionReader(ReaderConfiguration.getConfiguration().getCollectionsFolder(), COLLECTION_ID);
    }

    @Test
    public void testGetAvailableContent() throws ZebedeeException, IOException {
        Page content = collectionReader.getContent("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16");
        assertNotNull(content);
        assertEquals(content.getType(), PageType.article);
        assertTrue(content instanceof Article);
    }


    @Test
    public void testGetAvailableContentInWelsh() throws ZebedeeException, IOException {
        collectionReader.setLanguage(ContentLanguage.cy);
        Page content = collectionReader.getContent("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16");
        assertNotNull(content);
        assertEquals(content.getType(), PageType.article);
        assertTrue(content instanceof Article);
        assertEquals("prif bwyntiau", content.getDescription().getTitle());
    }

    @Test
    public void testFallbackToEnglish() throws ZebedeeException, IOException {
        collectionReader.setLanguage(ContentLanguage.cy);
        Page content = collectionReader.getContent("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02/");
        assertNotNull(content);
        assertEquals(content.getType(), PageType.dataset_landing_page);
        assertTrue(content instanceof DatasetLandingPage);
    }

    @Test(expected = NotFoundException.class)
    public void testGetNonexistingContent() throws ZebedeeException, IOException {
        Page content = collectionReader.getContent("madeupfoldername");
    }

    @Test(expected = NotFoundException.class)
    public void testGetNonexistingContentInWelsh() throws ZebedeeException, IOException {
        collectionReader.setLanguage(ContentLanguage.cy);
        Page content = collectionReader.getContent("madeupfoldername");
    }

    @Test
    public void testStartingWithForwardSlash() throws ZebedeeException, IOException {
        try (Resource resource = collectionReader.getResource("/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/0c908062.html")) {
            assertNotNull(resource);
        }
    }

    @Test
    public void testXlsResource() throws ZebedeeException, IOException {
        try (Resource resource = collectionReader.getResource("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02/labd02jul2015_tcm77-408195.xls")) {
            assertNotNull(resource != null);
//            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    @Test
    public void testXlsResourceInWelsh() throws ZebedeeException, IOException {
        //welsh language should not affect resource reads
        collectionReader.setLanguage(ContentLanguage.cy);
        try (Resource resource = collectionReader.getResource("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02/labd02jul2015_tcm77-408195.xls")) {
            assertNotNull(resource != null);
//            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    @Test
    public void testPngResource() throws ZebedeeException, IOException {
        try (Resource resource = collectionReader.getResource("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/96db1c4e.png/")) {
            assertNotNull(resource != null);
//            assertEquals("image/png", resource.getMimeType());
            assertTrue(resource.getData().available() > 0);
        }
    }

    @Test
    public void testHtmlResource() throws ZebedeeException, IOException {
        try (Resource resource = collectionReader.getResource("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/5b8d62b4.html")) {
            assertNotNull(resource != null);
//            assertEquals("text/html", resource.getMimeType());
            assertTrue(resource.getData().available() > 0);
        }
    }

    @Test
    public void testGetChildrenDirectories() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = collectionReader.getChildren("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets");
        assertTrue(children.size() == 1);
        Map.Entry<URI, ContentNode> contentNode = children.entrySet().iterator().next();
        assertEquals("Labour disputes by sector: LABD02", contentNode.getValue().getDescription().getTitle());
        assertEquals(PageType.dataset_landing_page, contentNode.getValue().getType());//type is null for directories with no data.json
        assertEquals("/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02", contentNode.getKey().toString());
    }


    @Test
    public void testGetChildrenInWelsh() throws ZebedeeException, IOException {
        collectionReader.setLanguage(ContentLanguage.cy);
        Map<URI, ContentNode> children = collectionReader.getChildren("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes");
        URI labourDisputes = URI.create("/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2014-07-16");
        URI prifBwyntiau = URI.create("/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16");

        assertTrue(children.containsKey(labourDisputes));
        assertTrue(children.containsKey(prifBwyntiau));
        ContentNode englishContent = children.get(labourDisputes);// no welsh available for this one
        ContentNode welshContent = children.get(prifBwyntiau);// no welsh available for this one

        assertEquals("Labour disputes", englishContent.getDescription().getTitle());
        assertEquals("annual article 2014",englishContent.getDescription().getEdition());

        assertEquals("prif bwyntiau", welshContent.getDescription().getTitle());
        assertEquals("erthygl blynyddol 2015",welshContent.getDescription().getEdition());
    }

    @Test
    public void testNonExistingNodeChilren() throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = collectionReader.getChildren("/nonexistingpath/test");
        assertTrue(children.isEmpty());
    }

    @Test
    public void testGetParents() throws ZebedeeException, IOException {
        Map<URI, ContentNode> parents = collectionReader.getParents("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions");
        assertTrue(parents.size() == 1);
    }


}
