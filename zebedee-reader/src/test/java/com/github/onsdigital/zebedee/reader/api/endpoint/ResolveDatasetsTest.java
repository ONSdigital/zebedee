package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.onsdigital.zebedee.content.page.home.HomePage;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.resolver.DatasetSummaryResolver;
import com.github.onsdigital.zebedee.reader.util.ResponseWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        api = new ResolveDatasets(datasetSummaryResolver, responseWriter, (lang) -> handler);
    }

    @Test
    public void shouldReturnBadRequestIfURIParamIsNull() throws Exception {
        api.handle(request, response);

        ArgumentCaptor<BadRequestException> captor = ArgumentCaptor.forClass(BadRequestException.class);

        verify(responseWriter, times(1)).sendBadRequest(captor.capture(), eq(request), eq(response));
        verifyZeroInteractions(handler, datasetSummaryResolver);

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
        verifyZeroInteractions(datasetSummaryResolver);

        assertThat(captor.getValue().getMessage(), equalTo("invalid page type for getDatasetSummaries datasets"));
    }
}
