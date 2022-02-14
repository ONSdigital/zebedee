package com.github.onsdigital.zebedee.reader.resolver;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.api.bean.DatasetSummary;
import dp.api.dataset.DatasetAPIClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.model.Dataset;
import dp.api.dataset.model.DatasetLinks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DatasetSummaryResolverTest {

    private static final String PAGE_URI = "/economy/economicoutputandproductivity/output";
    private static final String CPIH01_ID = "cpih01";

    private Link cmdDatasetLink;
    private Link legacyDatasetLink;

    @Mock
    private DatasetAPIClient datasetAPIClient;

    @Mock
    private ReadRequestHandler handler;

    @Mock
    private HttpServletRequest request;

    private DatasetSummaryResolver resolver;
    private Dataset cpihDataset;
    private DatasetLandingPage datasetLandingPage;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        this.cmdDatasetLink = new Link(new URI("/datasets/" + CPIH01_ID));
        this.legacyDatasetLink = new Link(new URI("/economy/economicoutputandproductivity/output/datasets/indexofproduction"));

        dp.api.dataset.model.Link selfLink = new dp.api.dataset.model.Link();
        selfLink.setHref("/datasets/" + CPIH01_ID);

        DatasetLinks links = new DatasetLinks();
        links.setSelf(selfLink);

        this.cpihDataset = new Dataset();
        cpihDataset.setLinks(links);
        cpihDataset.setTitle("War Pigs");
        cpihDataset.setDescription(
                "Generals gathered in their masses\n" +
                        "Just like witches at black masses\n" +
                        "Evil minds that plot destruction\n" +
                        "Sorcerers of death's construction\n" +
                        "In the fields the bodies burning\n" +
                        "As the war machine keeps turning\n" +
                        "Death and hatred to mankind\n" +
                        "Poisoning their brainwashed minds\n" +
                        "Oh lord yeah!");

        PageDescription description = new PageDescription();
        description.setTitle("Raining Blood");
        description.setSummary(
                "Trapped in purgatory\n" +
                        "A lifeless object, alive\n" +
                        "Awaiting reprisal\n" +
                        "Death will be their acquiescence\n" +
                        "The sky is turning red\n" +
                        "Return to power draws near\n" +
                        "Fall into me, the sky's crimson tears\n" +
                        "Abolish the rules made of stone\n" +
                        "Pierced from below, souls of my treacherous past\n" +
                        "Betrayed by many, now ornaments dripping above\n" +
                        "Awaiting the hour of reprisal\n" +
                        "Your time slips away\n" +
                        "Raining blood\n" +
                        "From a lacerated sky\n" +
                        "Bleeding its horror\n" +
                        "Creating my structure now I shall reign in blood");
        datasetLandingPage = new DatasetLandingPage();
        datasetLandingPage.setDescription(description);
        datasetLandingPage.setUri(new URI("/economy/economicoutputandproductivity/output/datasets/indexofproduction"));
    }

    @Test
    public void testResolveLegacyDataset_ReadRequestHandlerException() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(handler.getContent(legacyDatasetLink.getUri().toString(), request))
                .thenThrow(new InternalServerError("", null));

        DatasetSummary result = resolver.resolve(PAGE_URI, legacyDatasetLink, request, handler);

        assertThat(result, is(nullValue()));
        verify(handler, times(1)).getContent(legacyDatasetLink.getUri().toString(), request);
        verifyNoInteractions(datasetAPIClient);
    }

    @Test
    public void testResolveLegacyDataset_Success() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(handler.getContent(legacyDatasetLink.getUri().toString(), request))
                .thenReturn(datasetLandingPage);

        DatasetSummary result = resolver.resolve(PAGE_URI, legacyDatasetLink, request, handler);

        assertThat(result.getUri(), equalTo(datasetLandingPage.getUri().toString()));
        assertThat(result.getTitle(), equalTo(datasetLandingPage.getDescription().getTitle()));
        assertThat(result.getSummary(), equalTo(datasetLandingPage.getDescription().getSummary()));
        verify(handler, times(1)).getContent(legacyDatasetLink.getUri().toString(), request);
        verifyNoInteractions(datasetAPIClient);
    }

    @Test
    public void testResolveCMDDataset_DatasetAPIClientException() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenThrow(new IOException(""));

        DatasetSummary result = resolver.resolve(PAGE_URI, cmdDatasetLink, request, handler);

        assertThat(result, is(nullValue()));
        verify(datasetAPIClient, times(1)).getDataset(CPIH01_ID);
        verifyNoInteractions(handler);
    }

    @Test
    public void testResolveCMDDataset_CMDFeatureDisabled() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, false);

        DatasetSummary result = resolver.resolve(PAGE_URI, cmdDatasetLink, request, handler);

        assertThat(result, is(nullValue()));
        verifyNoInteractions(handler, datasetAPIClient);
    }

    @Test
    public void testResolveCMDDataset_Success() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenReturn(cpihDataset);

        DatasetSummary result = resolver.resolve(PAGE_URI, cmdDatasetLink, request, handler);

        assertThat(result.getSummary(), equalTo(cpihDataset.getDescription()));
        assertThat(result.getTitle(), equalTo(cpihDataset.getTitle()));
        assertThat(result.getUri(), equalTo(cpihDataset.getLinks().getSelf().getHref()));
        verify(datasetAPIClient, times(1)).getDataset(CPIH01_ID);
        verifyNoInteractions(handler);
    }

    @Test
    public void testGetLegacyDatasetSummary_IOException() throws Exception {
        resolver = new DatasetSummaryResolver(null, false);

        when(handler.getContent(legacyDatasetLink.getUri().toString(), request))
                .thenThrow(new IOException(""));

        DatasetSummary result = resolver.getLegacyDatasetSummary(PAGE_URI, legacyDatasetLink, request, handler);
        assertThat(result, is(nullValue()));
        verify(handler, times(1)).getContent(legacyDatasetLink.getUri().toString(), request);
    }

    @Test
    public void testGetLegacyDatasetSummary_ZebedeeException() throws Exception {
        resolver = new DatasetSummaryResolver(null, false);

        when(handler.getContent(legacyDatasetLink.getUri().toString(), request))
                .thenThrow(new InternalServerError("", null));

        DatasetSummary result = resolver.getLegacyDatasetSummary(PAGE_URI, legacyDatasetLink, request, handler);
        assertThat(result, is(nullValue()));
        verify(handler, times(1)).getContent(legacyDatasetLink.getUri().toString(), request);
    }

    @Test
    public void testGetLegacyDatasetSummary_Success() throws Exception {
        resolver = new DatasetSummaryResolver(null, false);

        when(handler.getContent(legacyDatasetLink.getUri().toString(), request))
                .thenReturn(datasetLandingPage);

        DatasetSummary result = resolver.getLegacyDatasetSummary(PAGE_URI, legacyDatasetLink, request, handler);
        assertThat(result.getUri(), equalTo(datasetLandingPage.getUri().toString()));
        assertThat(result.getTitle(), equalTo(datasetLandingPage.getDescription().getTitle()));
        assertThat(result.getSummary(), equalTo(datasetLandingPage.getDescription().getSummary()));
        verify(handler, times(1)).getContent(legacyDatasetLink.getUri().toString(), request);
    }

    @Test
    public void testGetCMDDatasetSummary_ClientDatasetAPIException() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenThrow(new ClientException(""));

        DatasetSummary result = resolver.getCMDDatasetSummary(PAGE_URI, cmdDatasetLink);
        assertThat(result, is(nullValue()));
        verify(datasetAPIClient, times(1)).getDataset(CPIH01_ID);
    }

    @Test
    public void testGetCMDDatasetSummary_ClientIOException() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenThrow(new IOException(""));

        DatasetSummary result = resolver.getCMDDatasetSummary(PAGE_URI, cmdDatasetLink);
        assertThat(result, is(nullValue()));
        verify(datasetAPIClient, times(1)).getDataset(CPIH01_ID);
    }

    @Test
    public void testGetCMDDatasetSummary_InvalidLink() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenReturn(cpihDataset);

        DatasetSummary result = resolver.getCMDDatasetSummary(PAGE_URI, new Link(new URI("/datasets/")));
        assertThat(result, is(nullValue()));
        verify(datasetAPIClient, never()).getDataset(anyString());
    }

    @Test
    public void testGetCMDDatasetSummary_Success() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenReturn(cpihDataset);

        DatasetSummary result = resolver.getCMDDatasetSummary(PAGE_URI, cmdDatasetLink);

        assertThat(result.getSummary(), equalTo(cpihDataset.getDescription()));
        assertThat(result.getTitle(), equalTo(cpihDataset.getTitle()));
        assertThat(result.getUri(), equalTo(cpihDataset.getLinks().getSelf().getHref()));
        verify(datasetAPIClient, times(1)).getDataset(CPIH01_ID);
    }

    @Test
    public void testGetDatasetID_Success() throws Exception {
        resolver = new DatasetSummaryResolver(null, false);

        String id = resolver.getCMDDatasetID(PAGE_URI, cmdDatasetLink);
        assertThat(id, equalTo(CPIH01_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDatasetID_InvalidLink() throws Exception {
        resolver = new DatasetSummaryResolver(null, false);

        resolver.getCMDDatasetID(PAGE_URI, new Link(new URI("/datasets/")));
    }

    static class ClientException extends DatasetAPIException {
        public ClientException(String message) {
            super(message);
        }
    }
}
