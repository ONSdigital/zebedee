package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by bren on 30/07/15.
 */

/*Notice that resources must be generated for these tests to pass. Maven test phase runs after resources are generated.* */

public class CollectionContentReaderTest {

    private CollectionContentReader collectionReader;
    private final static String collectionId = "testcollection-testid";

    @Before
    public void createContentReader() {
        this.collectionReader = new CollectionContentReader("target/test-content/collections");
    }

    @Test
    public void testGetAvailableContent() throws ZebedeeException, IOException {
        Page content = collectionReader.getContent(collectionId,  "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/0c908062.json");
        assertNotNull(content);
        assertEquals(content.getType(), PageType.table);
        assertTrue(content instanceof Table);
    }

    @Test(expected = NotFoundException.class)
    public void testGetNonexistingContent() throws ZebedeeException, IOException {
        Page content = collectionReader.getContent(collectionId, "madeupfoldername/data.json");
    }

    @Test
    public void testStartingWithForwardSlash() throws ZebedeeException, IOException {
        try (Resource resource = collectionReader.getResource(collectionId, "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/0c908062.html")) {
            assertNotNull(resource);
        }
    }

    @Test
    public void testXlsResource() throws ZebedeeException, IOException {
        try (Resource resource = collectionReader.getResource(collectionId, "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02/labd02jul2015_tcm77-408195.xls")) {
            assertNotNull(resource != null);
//            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    @Test
    public void testPngResource() throws ZebedeeException, IOException {
        try (Resource resource = collectionReader.getResource(collectionId, "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/96db1c4e.png/")) {
            assertNotNull(resource != null);
//            assertEquals("image/png", resource.getMimeType());
            assertTrue(resource.getData().available() > 0);
        }
    }

    @Test
    public void testHtmlResource() throws ZebedeeException, IOException {
        try (Resource resource = collectionReader.getResource(collectionId, "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/5b8d62b4.html")) {
            assertNotNull(resource != null);
//            assertEquals("text/html", resource.getMimeType());
            assertTrue(resource.getData().available() > 0);
        }
    }

    @Test
    public void testGetChildrenDirectories() throws ZebedeeException, IOException {
        List<ContentNode> children = collectionReader.getChildren(collectionId, "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets");
        assertTrue(children.size() == 1);
        ContentNode contentNode = children.get(0);
        assertEquals("Labour disputes by sector: LABD02", contentNode.getDescription().getTitle());
        assertEquals(PageType.dataset, contentNode.getType());//type is null for directories with no data.json
        assertEquals("/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02/", contentNode.getUri().toString());
    }

    @Test(expected = NotFoundException.class)
    public void testNonExistingNodeChilren() throws ZebedeeException, IOException {
        List<ContentNode> children = collectionReader.getChildren(collectionId, "/nonexistingpath/test");
    }

}
