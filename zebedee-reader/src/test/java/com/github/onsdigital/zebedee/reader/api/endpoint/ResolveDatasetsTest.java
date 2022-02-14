package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.onsdigital.zebedee.content.page.home.HomePage;
import com.github.onsdigital.zebedee.content.page.taxonomy.ProductPage;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.api.bean.DatasetSummary;
import com.github.onsdigital.zebedee.reader.resolver.DatasetSummaryResolver;
import com.github.onsdigital.zebedee.reader.util.ResponseWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ResolveDatasetsTest {

    private static final String URI = "/a/b/c";

    @Mock
    private DatasetSummaryResolver datasetSummaryResolver;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ReadRequestHandler handler;

    @Mock
    private ResponseWriter responseWriter;

    private ResolveDatasets api;

    private ProductPage productPage;

    private List<Link> links;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        api = new ResolveDatasets(datasetSummaryResolver, responseWriter, (lang) -> handler);
    }

    @Test
    public void shouldReturnBadRequestIfURIParamIsNull() throws Exception {
        api.handle(request, response);

        ArgumentCaptor<BadRequestException> captor = ArgumentCaptor.forClass(BadRequestException.class);

        verify(responseWriter, times(1)).sendBadRequest(captor.capture(), eq(request), eq(response));
        verifyNoInteractions(handler, datasetSummaryResolver);

        assertThat(captor.getValue().getMessage(), equalTo("uri parameter is required but was not specified"));
    }

    @Test
    public void shouldReturnBadRequestForNonProductPages() throws Exception {
        when(request.getParameter("uri"))
                .thenReturn(URI);
        when(handler.findContent(request, null))
                .thenReturn(new HomePage());

        api.handle(request, response);

        ArgumentCaptor<BadRequestException> captor = ArgumentCaptor.forClass(BadRequestException.class);

        verify(handler, times(1)).findContent(request, null);
        verify(responseWriter, times(1)).sendBadRequest(captor.capture(), eq(request), eq(response));
        verifyNoInteractions(datasetSummaryResolver);

        assertThat(captor.getValue().getMessage(), equalTo("invalid page type for getDatasetSummaries datasets"));
    }

    @Test
    public void shouldReturnEmptyIfDatasetsListNull() throws Exception {
        when(request.getParameter("uri"))
                .thenReturn(URI);

        productPage = new ProductPage();
        productPage.setUri(new URI(URI));

        when(handler.findContent(request, null))
                .thenReturn(productPage);

        api.handle(request, response);

        verify(handler, times(1)).findContent(request, null);
        verify(responseWriter, times(1)).sendResponse(new ArrayList<DatasetSummary>(), response);
        verifyNoInteractions(datasetSummaryResolver);
    }

    @Test
    public void shouldReturnEmptyIfDatasetsListEmpty() throws Exception {
        when(request.getParameter("uri"))
                .thenReturn(URI);

        productPage = new ProductPage();
        productPage.setUri(new URI(URI));
        productPage.setDatasets(new ArrayList<>());

        when(handler.findContent(request, null))
                .thenReturn(productPage);

        api.handle(request, response);

        verify(handler, times(1)).findContent(request, null);
        verify(responseWriter, times(1)).sendResponse(new ArrayList<DatasetSummary>(), response);
        verifyNoInteractions(datasetSummaryResolver);
    }

    @Test
    public void shouldOmittNullEntries() throws Exception {
        when(request.getParameter("uri"))
                .thenReturn(URI);

        Link datasetLink = new Link(new URI("/datasets/cpih01"));

        links = new ArrayList<>();
        links.add(datasetLink);

        productPage = new ProductPage();
        productPage.setUri(new URI(URI));
        productPage.setDatasets(links);

        when(handler.findContent(request, null))
                .thenReturn(productPage);
        when(datasetSummaryResolver.resolve(URI, datasetLink, request, handler))
                .thenReturn(null);

        api.handle(request, response);

        verify(handler, times(1)).findContent(request, null);
        verify(responseWriter, times(1)).sendResponse(new ArrayList<DatasetSummary>(), response);
        verify(datasetSummaryResolver, times(1)).resolve(URI, datasetLink, request, handler);
    }

    @Test
    public void shouldReturnExpectedSummaries() throws Exception {
        when(request.getParameter("uri"))
                .thenReturn(URI);

        Link cpih01 = new Link(new URI("/datasets/cpih01"));
        Link cpih02 = new Link(new URI("/datasets/cpih02"));

        links = new ArrayList<>();
        links.add(cpih01);
        links.add(cpih02);

        productPage = new ProductPage();
        productPage.setUri(new URI(URI));
        productPage.setDatasets(links);

        DatasetSummary summaryCpih01 = new DatasetSummary();
        summaryCpih01.setUri(cpih01.getUri().toString());

        DatasetSummary summaryCpih02 = new DatasetSummary();
        summaryCpih02.setUri(cpih02.getUri().toString());

        List<DatasetSummary> summaries = new ArrayList() {{
            add(summaryCpih01);
            add(summaryCpih02);
        }};

        when(handler.findContent(request, null))
                .thenReturn(productPage);
        when(datasetSummaryResolver.resolve(URI, cpih01, request, handler))
                .thenReturn(summaryCpih01);
        when(datasetSummaryResolver.resolve(URI, cpih02, request, handler))
                .thenReturn(summaryCpih02);

        api.handle(request, response);

        verify(handler, times(1)).findContent(request, null);
        verify(datasetSummaryResolver, times(1)).resolve(URI, cpih01, request, handler);
        verify(datasetSummaryResolver, times(1)).resolve(URI, cpih02, request, handler);
        verify(responseWriter, times(1)).sendResponse(summaries, response);
    }
}
