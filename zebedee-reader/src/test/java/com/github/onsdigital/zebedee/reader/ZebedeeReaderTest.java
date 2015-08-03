package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.StaticPage;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table;
import com.github.onsdigital.zebedee.content.dynamic.TitleWrapper;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by bren on 31/07/15.
 */

public class ZebedeeReaderTest {


    //TODO: mime type resolving not working on Mac machines due to java bug in Files.probeContentType, use a lib to resolve mime type based on extension and enable test bits checking mime types

    @Before
    public void initializeTestConfig() {
        ReaderConfiguration.init("target/test-content");
    }

    @Test
    public void testReadPublishedContent() throws ZebedeeException, IOException {
        Page content = readAccessibilityData("about/accessibility/data.json///");
    }

    @Test
    public void testReadPublishedContentWithAbsoluteUri() throws ZebedeeException, IOException {
        Page accessibilityData = readAccessibilityData("/about/accessibility/data.json///");
    }

    @Test
    public void testTitleFilter() throws ZebedeeException, IOException {
        Content content = ZebedeeReader.getInstance().getPublishedContent("about/accessibility/data.json", DataFilter.TITLE);
        assertNotNull(content);
        assertTrue(content instanceof TitleWrapper);
        TitleWrapper titleWrapper = (TitleWrapper) content;
        assertNotNull(titleWrapper);
        assertEquals("Accessibility", titleWrapper.getTitle());
    }

    @Test
    public void testDescriptionFilter() throws ZebedeeException, IOException {
        Content content = ZebedeeReader.getInstance().getCollectionContent("testcollection-testid", "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/datasets/labourdisputesbysectorlabd02/data.json", DataFilter.DESCRIPTION);
        assertNotNull(content);
        assertTrue(content instanceof PageDescription);
        PageDescription description = (PageDescription) content;
        assertEquals("Labour disputes by sector: LABD02", description.getTitle());
        assertEquals("Richard Clegg", description.getContact().getName());
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
        Content content = ZebedeeReader.getInstance().getCollectionContent("testcollection-testid", "employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/0c908062.json");
        assertNotNull(content);
        assertTrue(content instanceof Page);
        Page page = (Page) content;
        assertEquals(page.getType(), PageType.table);
        assertTrue(content instanceof Table);
    }

    @Test(expected = CollectionNotFoundException.class)
    public void testNonExistingCollectionRead() throws ZebedeeException, IOException {
        Content content = ZebedeeReader.getInstance().getCollectionContent("nonexistingcollection", "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/0c908062.json");
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
        Content content = ZebedeeReader.getInstance().getPublishedContent(path);
        assertNotNull(content);
        return content;
    }

}
