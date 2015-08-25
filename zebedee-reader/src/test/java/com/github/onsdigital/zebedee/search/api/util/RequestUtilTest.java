package com.github.onsdigital.zebedee.search.api.util;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.when;

public class RequestUtilTest {

    @Test
    public void extractPageShouldReturn1IfPageNotGiven() {

        // Given an empty request with no page number given
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // When the extractPage method is called
        int actual = RequestUtil.extractPage(request);

        // Then a default page number of 1 is returned
        assertEquals(1, actual);
    }

    @Test
    public void extractPageShouldReturn1IfPageLessThan1() {

        // Given a request that contains a page query string parameter of 0.
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("page")).thenReturn("0");

        // When the extractPage method is called
        int actual = RequestUtil.extractPage(request);

        // Then a default page number of 1 is returned
        assertEquals(1, actual);
    }

    @Test
    public void extractPageShouldReturn1IfPageLessThan0() {

        // Given a request that contains a page query string parameter of -3.
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("page")).thenReturn("-3");

        // When the extractPage method is called
        int actual = RequestUtil.extractPage(request);

        // Then a default page number of 1 is returned
        assertEquals(1, actual);
    }

    @Test
    public void extractPageShouldReturnPageNumberAsGiven() {

        // Given a request that contains a page query string parameter of 3.
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("page")).thenReturn("3");

        // When the extractPage method is called
        int actual = RequestUtil.extractPage(request);

        // Then a default page number of 1 is returned
        assertEquals(3, actual);
    }
}
