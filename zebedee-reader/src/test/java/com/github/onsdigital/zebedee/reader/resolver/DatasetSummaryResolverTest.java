package com.github.onsdigital.zebedee.reader.resolver;

import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.reader.api.bean.DatasetSummary;
import dp.api.dataset.DatasetAPIClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.model.Dataset;
import dp.api.dataset.model.DatasetLinks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatasetSummaryResolverTest {

    private static final String PAGE_URI = "/economy/economicoutputandproductivity/output";
    private static final String CPIH01_ID = "cpih01";

    private Link datasetLink;

    @Mock
    private DatasetAPIClient datasetAPIClient;

    private DatasetSummaryResolver resolver;
    private Dataset cpihDataset;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.datasetLink = new Link(new URI("/datasets/" + CPIH01_ID));

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
    }

    @Test
    public void testGetCMDDatasetSummary_clientDatasetAPIException() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenThrow(new ClientException(""));

        DatasetSummary result = resolver.getCMDDatasetSummary(PAGE_URI, datasetLink);
        assertThat(result, is(nullValue()));
        verify(datasetAPIClient, times(1)).getDataset(CPIH01_ID);
    }

    @Test
    public void testGetCMDDatasetSummary_clientIOException() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenThrow(new IOException(""));

        DatasetSummary result = resolver.getCMDDatasetSummary(PAGE_URI, datasetLink);
        assertThat(result, is(nullValue()));
        verify(datasetAPIClient, times(1)).getDataset(CPIH01_ID);
    }

    @Test
    public void testGetCMDDatasetSummary_invalidLink() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenReturn(cpihDataset);

        DatasetSummary result = resolver.getCMDDatasetSummary(PAGE_URI, new Link(new URI("/datasets/")));
        assertThat(result, is(nullValue()));
        verify(datasetAPIClient, never()).getDataset(anyString());
    }

    @Test
    public void testGetCMDDatasetSummary_success() throws Exception {
        resolver = new DatasetSummaryResolver(datasetAPIClient, true);

        when(datasetAPIClient.getDataset(CPIH01_ID))
                .thenReturn(cpihDataset);

        DatasetSummary result = resolver.getCMDDatasetSummary(PAGE_URI, datasetLink);

        assertThat(result.getSummary(), equalTo(cpihDataset.getDescription()));
        assertThat(result.getTitle(), equalTo(cpihDataset.getTitle()));
        assertThat(result.getUri(), equalTo(cpihDataset.getLinks().getSelf().getHref()));
        verify(datasetAPIClient, times(1)).getDataset(CPIH01_ID);
    }

    @Test
    public void testGetDatasetID_success() throws Exception {
        resolver = new DatasetSummaryResolver(null, false);

        String id = resolver.getCMDDatasetID(PAGE_URI, datasetLink);
        assertThat(id, equalTo(CPIH01_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDatasetID_invalidLink() throws Exception {
        resolver = new DatasetSummaryResolver(null, false);

        resolver.getCMDDatasetID(PAGE_URI, new Link(new URI("/datasets/")));
    }

    static class ClientException extends DatasetAPIException {
        public ClientException(String message) {
            super(message);
        }
    }
}
