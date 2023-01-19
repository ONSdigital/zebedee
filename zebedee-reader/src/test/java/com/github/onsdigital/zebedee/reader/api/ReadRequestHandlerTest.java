package com.github.onsdigital.zebedee.reader.api;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.dynamic.DescriptionWrapper;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.document.article.Article;
import com.github.onsdigital.zebedee.content.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.FakeCollectionReaderFactory;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import junit.framework.AssertionFailedError;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by bren on 04/08/15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReadRequestHandlerTest {

    private static final String COLLECTION_ID = "testcollection-3ff3cfe6437e1b2e8fdde2a664cd3f9de239dd99e9acb109557f4a1089e48bd7";
    private static final String SESSION_ID = "SESSION.ID";

    private ReadRequestHandler handler;

    @Mock
    private HttpServletRequest request;
    
    @BeforeClass
    public static void beforeClasss() {
        ReaderConfiguration cfg = ReaderConfiguration.init("target/test-classes/test-content/");
        ZebedeeReader.setCollectionReaderFactory(new FakeCollectionReaderFactory(cfg.getCollectionsDir()));
    }
    
    @AfterClass
    public static void afterClass() {
        ZebedeeReader.setCollectionReaderFactory(null);
    }

    @Before
    public void initialize() {
        handler = new ReadRequestHandler();
    }

    @Test
    public void testFindContentNoCollectionWithTitleFilter() throws Exception {
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17";
        when(request.getParameter("uri")).thenReturn(uri);

        DataFilter filter = new DataFilter(DataFilter.FilterType.TITLE);

        Content content = handler.findContent(request, filter);

        // It should read the content from published
        assertNotNull(content);
        assertTrue(content instanceof ContentNodeDetails);
        ContentNodeDetails titleWrapper = (ContentNodeDetails) content;
        assertEquals("UK Natural Capital Land Cover in the UK", titleWrapper.getTitle());
        assertEquals("2022", titleWrapper.getEdition());
        assertEquals(uri, titleWrapper.getUri().toString());
    }

    @Test
    public void testFindContentNoCollectionWithDescriptionFilter() throws Exception {
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17";
        when(request.getParameter("uri")).thenReturn(uri);
        
        DataFilter filter = new DataFilter(DataFilter.FilterType.DESCRIPTION);

        Content content = handler.findContent(request, filter);

        // It should read the content from published
        assertNotNull(content);
        assertTrue(content instanceof DescriptionWrapper);
        DescriptionWrapper description = (DescriptionWrapper) content;
        assertEquals(uri, description.getUri().toString());
        assertEquals("UK Natural Capital Land Cover in the UK", description.getDescription().getTitle());
        assertEquals("2022", description.getDescription().getEdition());
        String expectedMetaDescription = "We take a look at land cover ecosystem accounts for the United Kingdom (UK). The land cover accounts based on data from the Countryside Survey show that the land cover changed significantly in the UK between 1998 and 2007.";
        assertEquals(expectedMetaDescription, description.getDescription().getMetaDescription());
        assertFalse(description.getDescription().isNationalStatistic());
        assertEquals("Vahé Nafilyan", description.getDescription().getContact().getName());
        assertEquals("vahe.nafilyan@ons.gsi.gov.uk", description.getDescription().getContact().getEmail());
        assertEquals("+ 44 (0)1633 651764", description.getDescription().getContact().getTelephone());
    }

    @Test
    public void testFindContentNoCollectionNoFilter() throws Exception {
        DataFilter filter = null;
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17";
        when(request.getParameter("uri")).thenReturn(uri);

        Content content = handler.findContent(request, filter);

        // It should read the content from published
        assertNotNull(content);
        assertTrue(content instanceof Article);
        Article article = (Article) content;
        assertEquals(PageType.ARTICLE, article.getType());
        assertEquals(uri, article.getUri().toString());
        assertEquals("UK Natural Capital Land Cover in the UK", article.getDescription().getTitle());
        assertEquals("2022", article.getDescription().getEdition());
        String expectedMetaDescription = "We take a look at land cover ecosystem accounts for the United Kingdom (UK). The land cover accounts based on data from the Countryside Survey show that the land cover changed significantly in the UK between 1998 and 2007.";
        assertEquals(expectedMetaDescription, article.getDescription().getMetaDescription());
        assertFalse(article.getDescription().isNationalStatistic());
        assertEquals("Vahé Nafilyan", article.getDescription().getContact().getName());
        assertEquals("vahe.nafilyan@ons.gsi.gov.uk", article.getDescription().getContact().getEmail());
        assertEquals("+ 44 (0)1633 651764", article.getDescription().getContact().getTelephone());
    }

    @Test
    public void testFindContentWithContentInCollectionNoFilter() throws Exception {
        DataFilter filter = null;
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        Content content = handler.findContent(request, filter);

        // It should read the content from the collection
        assertNotNull(content);
        assertTrue(content instanceof Article);
        Article article = (Article) content;
        assertEquals(PageType.ARTICLE, article.getType());
        assertEquals(uri, article.getUri().toString());
        assertEquals("Edited in collection", article.getDescription().getTitle());
        assertEquals("2022-1", article.getDescription().getEdition());
        String expectedMetaDescription = "Edited meta description";
        assertEquals(expectedMetaDescription, article.getDescription().getMetaDescription());
        assertTrue(article.getDescription().isNationalStatistic());
        assertEquals("Test", article.getDescription().getContact().getName());
        assertEquals("test@ons.gov.uk", article.getDescription().getContact().getEmail());
        assertEquals("01633 651764", article.getDescription().getContact().getTelephone());
    }

    @Test
    public void testFindContentWithContentNotInCollectionNoFilter() throws Exception {
        DataFilter filter = null;
        String uri = "/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2013-06-26";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        Content content = handler.findContent(request, filter);

        // It should read the content from publishing as it isn't present in the collection
        assertNotNull(content);
        assertTrue(content instanceof Bulletin);
        Bulletin bulletin = (Bulletin) content;
        assertEquals(PageType.BULLETIN, bulletin.getType());
        assertEquals(uri, bulletin.getUri().toString());
        assertEquals("UK Environmental Accounts", bulletin.getDescription().getTitle());
        assertEquals("2013", bulletin.getDescription().getEdition());
        String expectedSummary = "Environmental accounts show how the environment contributes to the economy, the impacts that the economy has on the environment, and how society responds to environmental issues. They include natural capital accounts (oil and gas), physical accounts (fuel use, energy consumption, atmospheric emissions, material flows, and water), and monetary accounts (environmental taxes and environmental protection expenditure). For 2014, experimental natural capital accounts are also included.";
        assertEquals(expectedSummary, bulletin.getDescription().getSummary());
        assertTrue(bulletin.getDescription().isNationalStatistic());
        assertEquals("James Evans", bulletin.getDescription().getContact().getName());
        assertEquals("james.evans@ons.gsi.gov.uk", bulletin.getDescription().getContact().getEmail());
        assertEquals("+44(0) 1633 456644", bulletin.getDescription().getContact().getTelephone());
    }

    @Test
    public void testFindContentLatestNoCollectionNoFilter() throws Exception {
        DataFilter filter = null;
        String uri = "/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/latest";
        when(request.getParameter("uri")).thenReturn(uri);

        Content content = handler.findContent(request, filter);

        // It should read the latest content from publishing
        assertNotNull(content);
        assertTrue(content instanceof Bulletin);
        Bulletin bulletin = (Bulletin) content;
        assertEquals(PageType.BULLETIN, bulletin.getType());
        assertEquals("/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2015-07-09", bulletin.getUri().toString());
        assertEquals("UK Environmental Accounts", bulletin.getDescription().getTitle());
        assertEquals("2015", bulletin.getDescription().getEdition());
        String expectedSummary = "Measures the contribution of the environment to the economy, the impacts the economy has on the environment, and society's responses to environmental issues.";
        assertEquals(expectedSummary, bulletin.getDescription().getSummary());
        assertTrue(bulletin.getDescription().isNationalStatistic());
        assertEquals("Matthew Steel", bulletin.getDescription().getContact().getName());
        assertEquals("environment.accounts@ons.gsi.gov.uk", bulletin.getDescription().getContact().getEmail());
        assertEquals("+44 (0)1633 455680", bulletin.getDescription().getContact().getTelephone());
    }

    @Test
    public void testFindContentLatestWithContentNotInCollectionNoFilter() throws Exception {
        DataFilter filter = null;
        String uri = "/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts//latest";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        Content content = handler.findContent(request, filter);

        // It should read the latest content from publishing as it isn't present in the collection
        assertNotNull(content);
        assertTrue(content instanceof Bulletin);
        Bulletin bulletin = (Bulletin) content;
        assertEquals(PageType.BULLETIN, bulletin.getType());
        assertEquals("/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2015-07-09", bulletin.getUri().toString());
        assertEquals("UK Environmental Accounts", bulletin.getDescription().getTitle());
        assertEquals("2015", bulletin.getDescription().getEdition());
        String expectedSummary = "Measures the contribution of the environment to the economy, the impacts the economy has on the environment, and society's responses to environmental issues.";
        assertEquals(expectedSummary, bulletin.getDescription().getSummary());
        assertTrue(bulletin.getDescription().isNationalStatistic());
        assertEquals("Matthew Steel", bulletin.getDescription().getContact().getName());
        assertEquals("environment.accounts@ons.gsi.gov.uk", bulletin.getDescription().getContact().getEmail());
        assertEquals("+44 (0)1633 455680", bulletin.getDescription().getContact().getTelephone());
    }

    @Test
    public void testFindContentLatestWithContentInCollectionNoFilter() throws Exception {
        DataFilter filter = null;
        String uri = "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/latest";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        Content content = handler.findContent(request, filter);

        // It should read the latest content from the collection
        assertNotNull(content);
        assertTrue(content instanceof Article);
        Article article = (Article) content;
        assertEquals(PageType.ARTICLE, article.getType());
        assertEquals("/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16", article.getUri().toString());
        assertEquals("Labour disputes", article.getDescription().getTitle());
        assertEquals("annual article 2014", article.getDescription().getEdition());
        String expectedMetaDesc = "UK breakdown of labour disputes covering: number of working days lost and number of stoppages.";
        assertEquals(expectedMetaDesc, article.getDescription().getMetaDescription());
        assertTrue(article.getDescription().isNationalStatistic());
        assertEquals("James Tucker", article.getDescription().getContact().getName());
        assertEquals("james.tucker@ons.gsi.gov.uk", article.getDescription().getContact().getEmail());
        assertEquals("+44 (0)1633 456589", article.getDescription().getContact().getTelephone());
    }

    @Test(expected=NotFoundException.class)
    public void testFindContentNotFound() throws Exception {
        DataFilter filter = null;
        String uri = "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2022-09-18";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        handler.findContent(request, filter);
    }

    @Test
    public void testFindPublishedContentWithTitleFilter() throws Exception {
        DataFilter filter = new DataFilter(DataFilter.FilterType.TITLE);
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17";
        when(request.getParameter("uri")).thenReturn(uri);

        Content content = handler.findPublishedContent(request, filter);

        // It should read the content from published
        assertNotNull(content);
        assertTrue(content instanceof ContentNodeDetails);
        ContentNodeDetails titleWrapper = (ContentNodeDetails) content;
        assertEquals("UK Natural Capital Land Cover in the UK", titleWrapper.getTitle());
        assertEquals("2022", titleWrapper.getEdition());
        assertEquals(uri, titleWrapper.getUri().toString());
    }

    @Test
    public void testFindPublishedContentWithDescriptionFilter() throws Exception {
        DataFilter filter = new DataFilter(DataFilter.FilterType.DESCRIPTION);
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17";
        when(request.getParameter("uri")).thenReturn(uri);

        Content content = handler.findPublishedContent(request, filter);

        // It should read the content from published
        assertNotNull(content);
        assertTrue(content instanceof DescriptionWrapper);
        DescriptionWrapper description = (DescriptionWrapper) content;
        assertEquals(uri, description.getUri().toString());
        assertEquals("UK Natural Capital Land Cover in the UK", description.getDescription().getTitle());
        assertEquals("2022", description.getDescription().getEdition());
        String expectedMetaDescription = "We take a look at land cover ecosystem accounts for the United Kingdom (UK). The land cover accounts based on data from the Countryside Survey show that the land cover changed significantly in the UK between 1998 and 2007.";
        assertEquals(expectedMetaDescription, description.getDescription().getMetaDescription());
        assertFalse(description.getDescription().isNationalStatistic());
        assertEquals("Vahé Nafilyan", description.getDescription().getContact().getName());
        assertEquals("vahe.nafilyan@ons.gsi.gov.uk", description.getDescription().getContact().getEmail());
        assertEquals("+ 44 (0)1633 651764", description.getDescription().getContact().getTelephone());
    }

    @Test
    public void testFindPublishedContentNoFilter() throws Exception {
        DataFilter filter = null;
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17";
        when(request.getParameter("uri")).thenReturn(uri);

        Content content = handler.findPublishedContent(request, filter);

        // It should read the published content
        assertNotNull(content);
        assertTrue(content instanceof Article);
        Article article = (Article) content;
        assertEquals(PageType.ARTICLE, article.getType());
        assertEquals(uri, article.getUri().toString());
        assertEquals("UK Natural Capital Land Cover in the UK", article.getDescription().getTitle());
        assertEquals("2022", article.getDescription().getEdition());
        String expectedMetaDescription = "We take a look at land cover ecosystem accounts for the United Kingdom (UK). The land cover accounts based on data from the Countryside Survey show that the land cover changed significantly in the UK between 1998 and 2007.";
        assertEquals(expectedMetaDescription, article.getDescription().getMetaDescription());
        assertFalse(article.getDescription().isNationalStatistic());
        assertEquals("Vahé Nafilyan", article.getDescription().getContact().getName());
        assertEquals("vahe.nafilyan@ons.gsi.gov.uk", article.getDescription().getContact().getEmail());
        assertEquals("+ 44 (0)1633 651764", article.getDescription().getContact().getTelephone());
    }

    @Test
    public void testFindPublishedContentLatest() throws Exception {
        DataFilter filter = null;
        String uri = "/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/latest";
        when(request.getParameter("uri")).thenReturn(uri);

        Content content = handler.findPublishedContent(request, filter);

        // It should read the latest content from publishing as it isn't present in the collection
        assertNotNull(content);
        assertTrue(content instanceof Bulletin);
        Bulletin bulletin = (Bulletin) content;
        assertEquals(PageType.BULLETIN, bulletin.getType());
        assertEquals("/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2015-07-09", bulletin.getUri().toString());
        assertEquals("UK Environmental Accounts", bulletin.getDescription().getTitle());
        assertEquals("2015", bulletin.getDescription().getEdition());
        String expectedSummary = "Measures the contribution of the environment to the economy, the impacts the economy has on the environment, and society's responses to environmental issues.";
        assertEquals(expectedSummary, bulletin.getDescription().getSummary());
        assertTrue(bulletin.getDescription().isNationalStatistic());
        assertEquals("Matthew Steel", bulletin.getDescription().getContact().getName());
        assertEquals("environment.accounts@ons.gsi.gov.uk", bulletin.getDescription().getContact().getEmail());
        assertEquals("+44 (0)1633 455680", bulletin.getDescription().getContact().getTelephone());
    }

    @Test(expected=NotFoundException.class)
    public void testFindPublishedNotFound() throws Exception {
        DataFilter filter = null;
        // Present in the collection but not in published
        String uri = "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16";
        when(request.getParameter("uri")).thenReturn(uri);

        handler.findPublishedContent(request, filter);
    }

    @Test
    public void testGetContentInCollection() throws Exception {
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17";
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        Content content = handler.getContent(uri, request);

        // It should read the content from the collection
        assertNotNull(content);
        assertTrue(content instanceof Article);
        Article article = (Article) content;
        assertEquals(PageType.ARTICLE, article.getType());
        assertEquals(uri, article.getUri().toString());
        assertEquals("Edited in collection", article.getDescription().getTitle());
        assertEquals("2022-1", article.getDescription().getEdition());
        String expectedMetaDescription = "Edited meta description";
        assertEquals(expectedMetaDescription, article.getDescription().getMetaDescription());
        assertTrue(article.getDescription().isNationalStatistic());
        assertEquals("Test", article.getDescription().getContact().getName());
        assertEquals("test@ons.gov.uk", article.getDescription().getContact().getEmail());
        assertEquals("01633 651764", article.getDescription().getContact().getTelephone());
    }

    @Test
    public void testGetContentNotInCollection() throws Exception {
        String uri = "/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2013-06-26";
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        Content content = handler.getContent(uri, request);

        // It should read the content from published as it isn't present in the collection
        assertNotNull(content);
        assertTrue(content instanceof Bulletin);
        Bulletin bulletin = (Bulletin) content;
        assertEquals(PageType.BULLETIN, bulletin.getType());
        assertEquals(uri, bulletin.getUri().toString());
        assertEquals("UK Environmental Accounts", bulletin.getDescription().getTitle());
        assertEquals("2013", bulletin.getDescription().getEdition());
        String expectedSummary = "Environmental accounts show how the environment contributes to the economy, the impacts that the economy has on the environment, and how society responds to environmental issues. They include natural capital accounts (oil and gas), physical accounts (fuel use, energy consumption, atmospheric emissions, material flows, and water), and monetary accounts (environmental taxes and environmental protection expenditure). For 2014, experimental natural capital accounts are also included.";
        assertEquals(expectedSummary, bulletin.getDescription().getSummary());
        assertTrue(bulletin.getDescription().isNationalStatistic());
        assertEquals("James Evans", bulletin.getDescription().getContact().getName());
        assertEquals("james.evans@ons.gsi.gov.uk", bulletin.getDescription().getContact().getEmail());
        assertEquals("+44(0) 1633 456644", bulletin.getDescription().getContact().getTelephone());
    }

    @Test(expected=NotFoundException.class)
    public void testGetContentdNotFound() throws Exception {
        String uri = "/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2022-06-26";
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        handler.getContent(uri, request);
    }

    @Test
    public void testFindResourceNoCollection() throws Exception {
        String uri = "/peoplepopulationandcommunity/culturalidentity/ethnicity/articles/ethnicityandthelabourmarket2011censusenglandandwales/2014-11-13/11170eaa.json";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn(null);

        Resource r = handler.findResource(request);

        // It should find the resource in published
        assertNotNull(r);
        assertEquals("11170eaa.json", r.getName());
        assertEquals(uri, r.getUri().toString());
        assertEquals("application/json", r.getMimeType());
    }

    @Test
    public void testFindResourcePresentInCollection() throws Exception {
        String uri = "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/211caf1f.png";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        Resource r = handler.findResource(request);

        // It should find the resource in the collection
        assertNotNull(r);
        assertEquals("211caf1f.png", r.getName());
        assertEquals(uri, r.getUri().toString());
        assertEquals("image/png", r.getMimeType());
    }

    @Test
    public void testFindResourceNotPresentInCollection() throws Exception {
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/4f5b14cb.xls";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        Resource r = handler.findResource(request);

        // It should not find the resource in the collection but in published
        assertNotNull(r);
        assertEquals("4f5b14cb.xls", r.getName());
        assertEquals(uri, r.getUri().toString());
        assertEquals("application/vnd.ms-excel", r.getMimeType());
    }

    @Test(expected = NotFoundException.class)
    public void testFindResourceNotFound() throws Exception {
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/notfound";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        handler.findResource(request);
    }

    @Test
    public void testGetContentLengthNoCollection() throws Exception {
        String uri = "/peoplepopulationandcommunity/culturalidentity/ethnicity/articles/ethnicityandthelabourmarket2011censusenglandandwales/2014-11-13/11170eaa.json";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn(null);

        long length = handler.getContentLength(request);

        // It should find the resource in published
        assertEquals(8078L, length);
    }

    @Test
    public void testGetContentLengthInCollection() throws Exception {
        String uri = "/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions/articles/labourdisputes/2015-07-16/211caf1f.png";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        long length = handler.getContentLength(request);

        // It should find the resource in the collection
        assertEquals(44875L, length);
    }

    @Test
    public void testGetContentLengthNotPresentInCollection() throws Exception {
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/4f5b14cb.xls";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        long length = handler.getContentLength(request);

        // It should not find the resource in the collection but in published
        assertEquals(56320L, length);
    }

    @Test(expected = NotFoundException.class)
    public void testGetContentLengthNotFound() throws Exception {
        String uri = "/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17/notfound";
        when(request.getParameter("uri")).thenReturn(uri);
        when(request.getRequestURI()).thenReturn("/data/" + COLLECTION_ID + uri);
        when(request.getHeader("Authorization")).thenReturn(SESSION_ID);

        handler.getContentLength(request);
    }

    @Test
    public void testGetTaxonomy() throws Exception {
        shouldResolveTaxonomyFirstLevel();
        shouldReadTaxonomyInDepth();
        shouldFailReadingCollection();
    }

    //Collection reads should be available without zebedee cms module running
    private void shouldFailReadingCollection() throws Exception {
        when(request.getRequestURI()).thenReturn("/browsetree/"+COLLECTION_ID+"/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk");
        try {
            handler.getTaxonomy(request, 1);
            throw new AssertionFailedError("Collection read should have failed");
        } catch (UnauthorizedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void shouldResolveTaxonomyFirstLevel() throws Exception {
        Collection<ContentNode> children = handler.getTaxonomy(request, 1);
        assertTrue(children.size() == 5);
        ContentNode child = children.iterator().next();
        assertNull(child.getChildren());
        assertEquals("Economy", child.getDescription().getTitle());
    }

    private void shouldReadTaxonomyInDepth() throws Exception {
        Collection<ContentNode> children = handler.getTaxonomy(request, 2);
        Iterator<ContentNode> iterator = children.iterator();
        ContentNode economy = iterator.next();//economy
        ContentNode environmentalAccounts = economy.getChildren().iterator().next();
        assertEquals("environmentalaccounts", environmentalAccounts.getDescription().getTitle());
        assertNull(environmentalAccounts.getChildren());
    }

    @Test
    public void testGetParents() throws Exception {
        shouldOverlayCollectionPaths();
    }

    private void shouldOverlayCollectionPaths() throws IOException, ZebedeeException {
        when(request.getHeader(RequestUtils.FLORENCE_TOKEN_HEADER)).thenReturn("any token is fine in test");
        when(request.getParameter("uri")).thenReturn("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions");
        when(request.getRequestURI()).thenReturn("/breadcrumb/"+COLLECTION_ID+"/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions");
        Collection<ContentNode> parents = handler.getParents(request);
        assertTrue(parents.size() == 2);
        Iterator<ContentNode> iterator = parents.iterator();
        ContentNode home = iterator.next();
        ContentNode employmentLabourMarket = iterator.next();
        assertEquals(URI.create("/"), home.getUri());
        assertEquals(URI.create("/employmentandlabourmarket"), employmentLabourMarket.getUri());
        //Collection content should be overwriting published content
        assertEquals("Employment and labour market-inprogress", employmentLabourMarket.getDescription().getTitle());
    }

    @Test
    public void testIndexFile() throws Exception {
        when(request.getParameter("uri")).thenReturn("/visualisations/test/content");
        Resource resource = handler.findResource(request);
        assertEquals("/visualisations/test/content/index.html", resource.getUri().toString());
    }

    @Test
    public void testURIWithSpaces() throws Exception {
        when(request.getParameter("uri")).thenReturn("/visualisations/test/content/has spaces");
        Resource resource = handler.findResource(request);
        assertEquals("/visualisations/test/content/has%20spaces/index.html", resource.getUri().toString());
    }
}