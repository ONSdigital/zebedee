package com.github.onsdigital.zebedee.reader.api;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.dynamic.DescriptionWrapper;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import com.github.onsdigital.zebedee.reader.util.AuthorisationHandler;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by bren on 04/08/15.
 */
public class ReadRequestHandlerTest {

    private ReadRequestHandler readRequestHandler;
    HttpServletRequest request = mock(HttpServletRequest.class);

    @Before
    public void initialize() {
        ReaderConfiguration.init("target/test-content");
        readRequestHandler = new ReadRequestHandler();
    }

    @Test
    public void testFindContent() throws Exception {
        shouldFilterTitle();
        shouldFilterDescription();
    }


    private void shouldFilterTitle() throws Exception {
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17");
        Content content = readRequestHandler.findContent(request, DataFilter.TITLE);
        assertNotNull(content);
        assertTrue(content instanceof ContentNodeDetails);
        ContentNodeDetails titleWrapper = (ContentNodeDetails) content;
        assertEquals("UK Natural Capital Land Cover in the UK", titleWrapper.getTitle());
    }

    private void shouldFilterDescription() throws Exception {
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17");
        Content content = readRequestHandler.findContent(request, DataFilter.DESCRIPTION);
        assertNotNull(content);
        assertTrue(content instanceof DescriptionWrapper);
        DescriptionWrapper description = (DescriptionWrapper) content;
        assertFalse(description.getDescription().isNationalStatistic());
    }


    @Test
    public void testFindResource() throws Exception {

    }

    @Test
    public void testGetTaxonomy() throws Exception {
        shouldResolveTaxonomyFirstLevel();
        shouldReadTaxonomyInDepth();
        shouldFailReadingCollection();
    }

    //Collection reads should be available without zebedee cms module running
    private void shouldFailReadingCollection() throws Exception {
        ReadRequestHandler.setAuthorisationHandler(null);
        when(request.getRequestURI()).thenReturn("/browsetree/testcollection-testid/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk");
        try {
            readRequestHandler.getTaxonomy(request, 1);
            throw new AssertionFailedError("Collection read should have failed");
        } catch (UnauthorizedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void shouldResolveTaxonomyFirstLevel() throws Exception {
        Collection<ContentNode> children = readRequestHandler.getTaxonomy(request, 1);
        assertTrue(children.size() == 4);
        ContentNode child = children.iterator().next();
        assertNull(child.getChildren());
        assertEquals("Economy", child.getDescription().getTitle());
    }

    private void shouldReadTaxonomyInDepth() throws Exception {
        Collection<ContentNode> children = readRequestHandler.getTaxonomy(request, 2);
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
        when(request.getParameter("uri")).thenReturn("employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions");
        when(request.getRequestURI()).thenReturn("/breadcrumb/testcollection-testid/employmentandlabourmarket/peopleinwork/workplacedisputesandworkingconditions");
        ReadRequestHandler.setAuthorisationHandler(new AuthorisationHandler() {
            @Override
            public void authorise(HttpServletRequest request, String collectionId) throws IOException, UnauthorizedException, NotFoundException {
                return;
            }
        });
        Collection<ContentNode> parents = readRequestHandler.getParents(request);
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
    public void testExtractUri() throws Exception {

    }

    @Test
    public void testSetAuthorisationHandler() throws Exception {

    }
}