package com.github.onsdigital.zebedee.reader.api;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
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
        assertTrue(content instanceof PageDescription);
        PageDescription description = (PageDescription) content;
        assertFalse(description.isNationalStatistic());
    }


    @Test
    public void testFindResource() throws Exception {

    }

    @Test
    public void testListChildren() throws Exception {
        shouldResolveChildren();
        shouldResolveChildrenInDepth();
        shouldFailReadingCollection();
    }

    //Collection reads should be available without zebedee cms module running
    private void shouldFailReadingCollection() throws Exception {
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk");
        when(request.getRequestURI()).thenReturn("/browsetree/testcollection-testid/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk");
        try {
            readRequestHandler.listChildren(request, 1);
            throw new AssertionFailedError("Collection read should have failed");
        } catch (UnauthorizedException e) {
            System.out.println(e.getMessage());
        }
    }

    private void shouldResolveChildren() throws Exception {
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk");
        Collection<ContentNode> children = readRequestHandler.listChildren(request, 1);
        assertTrue(children.size() == 1);
        ContentNode child = children.iterator().next();
        assertNull(child.getChildren());
        assertEquals("UK Natural Capital Land Cover in the UK", child.getDescriptions().getTitle());
    }

    private void shouldResolveChildrenInDepth() throws Exception {
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts");
        Collection<ContentNode> children = readRequestHandler.listChildren(request, 3);
        assertTrue(children.size() == 2);
        Iterator<ContentNode> iterator = children.iterator();
        ContentNode articles = iterator.next();
        ContentNode bulletins = iterator.next();
        assertEquals("articles", articles.getDescriptions().getTitle());
        assertEquals("bulletins", bulletins.getDescriptions().getTitle());

        assertNotNull(bulletins.getChildren());
        assertTrue(bulletins.getChildren().isEmpty() == false);
        assertTrue(articles.getChildren().size() == 1);
        ContentNode grandChild = bulletins.getChildren().iterator().next();
        assertEquals("ukenvironmentalaccounts", grandChild.getDescriptions().getTitle());
        assertNotNull(grandChild.getChildren());
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
        assertEquals(URI.create("/employmentandlabourmarket/"), employmentLabourMarket.getUri());
        //Collection content should be overwriting published content
        assertEquals("Employment and labour market-inprogress", employmentLabourMarket.getDescriptions().getTitle());
    }

    @Test
    public void testExtractUri() throws Exception {

    }

    @Test
    public void testSetAuthorisationHandler() throws Exception {

    }
}