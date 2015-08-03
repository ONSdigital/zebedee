package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.StaticPage;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by bren on 31/07/15.
 */

public class ZebedeeReaderTest {

    @Before
    public void initializeTestConfig() {
        ReaderConfiguration.init("target/zebedee");
    }

    @Test
    public void testReadPublishedContent() throws ZebedeeException, IOException {
        Page content = readAccessibilityData("/about/accessibility/data.json///");
    }

    @Test
    public void testReadPublishedContentWithAbsoluteUri() throws ZebedeeException, IOException {
        Page content = readAccessibilityData("about/accessibility/data.json///");
    }

    @Test(expected = NotFoundException.class)
    public void testNonExistingContentRead() throws ZebedeeException, IOException {
        ZebedeeReader.getInstance().getPublishedContent("non/existing/path/");
    }

    @Test
    public void testReadPublishedResource() throws ZebedeeException, IOException {
        try (Resource resource = ZebedeeReader.getInstance().getPublishedResource("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/4f5b14cb.xls")) {
            assertNotNull(resource != null);
//            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    @Test
    public void testReadCollectionContent() throws ZebedeeException, IOException {
        Page content = ZebedeeReader.getInstance().getCollectionContent("testcollection-testid", "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/0c908062.json");
        assertNotNull(content);
        assertEquals(content.getType(), PageType.table);
        assertTrue(content instanceof Table);
    }

    @Test(expected = NotFoundException.class)
    public void testNonExistingCollectionRead() throws ZebedeeException, IOException {
        Page content = ZebedeeReader.getInstance().getCollectionContent("nonexistingcollection", "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/0c908062.json");
    }

    @Test
    public void testXlsResource() throws ZebedeeException, IOException {
        try (Resource resource = ZebedeeReader.getInstance().getCollectionResource("testcollection-testid", "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02/labd02jul2015_tcm77-408195.xls")) {
            assertNotNull(resource != null);
//            assertEquals("application/vnd.ms-excel", resource.getMimeType());
            assertTrue(resource.isNotEmpty());
        }
    }

    private Page readAccessibilityData(String path) throws ZebedeeException, IOException {
        Page content = ZebedeeReader.getInstance().getPublishedContent(path);
        assertNotNull(content);
        assertEquals(content.getType(), PageType.static_page);
        assertEquals("Accessibility", content.getDescription().getTitle());
        assertTrue(content instanceof StaticPage);
        StaticPage staticPage = (StaticPage) content;
        assertNotNull(staticPage.getMarkdown());
        return content;
    }

}
