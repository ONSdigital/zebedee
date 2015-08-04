package com.github.onsdigital.zebedee.reader.api;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.TitleWrapper;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

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
        readRequestHandler =  new ReadRequestHandler();
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
        assertTrue(content instanceof TitleWrapper);
        TitleWrapper titleWrapper = (TitleWrapper) content;
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
    private void shouldFailReadingCollection() throws  Exception{
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
        assertEquals("UK Natural Capital Land Cover in the UK", child.getDescription().getTitle());
    }

    private void shouldResolveChildrenInDepth() throws Exception {
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts");
        Collection<ContentNode> children = readRequestHandler.listChildren(request, 3);
        assertTrue(children.size() == 2);
        Iterator<ContentNode> iterator = children.iterator();
        ContentNode articles = iterator.next();
        ContentNode bulletins = iterator.next();
        assertEquals("articles", articles.getDescription().getTitle());
        assertEquals("bulletins", bulletins.getDescription().getTitle());

        assertNotNull(bulletins.getChildren());
        assertTrue(bulletins.getChildren().isEmpty() == false);
        assertTrue(articles.getChildren().size() == 1);
        ContentNode grandChild = bulletins.getChildren().iterator().next();
        assertEquals("ukenvironmentalaccounts", grandChild.getDescription().getTitle());
        assertNotNull(grandChild.getChildren());
    }

    @Test
    public void testExtractUri() throws Exception {

    }

    @Test
    public void testSetAuthorisationHandler() throws Exception {

    }
}