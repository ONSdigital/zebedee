package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.ApiDatasetLandingPage;
import dp.api.dataset.DatasetClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.model.Dataset;

import java.io.IOException;

/**
 * A PageUpdateHook implementation for when an ApiDatasetLandingPage is created.
 */
public class APIDatasetLandingPageCreationHook implements PageUpdateHook<ApiDatasetLandingPage> {

    private DatasetClient datasetAPIClient;

    /**
     * Create a new instance using the given dataset client.
     * @param datasetAPIClient
     */
    public APIDatasetLandingPageCreationHook(DatasetClient datasetAPIClient) {
        this.datasetAPIClient = datasetAPIClient;
    }

    /**
     * The method that gets run when a ApiDatasetLandingPage is updated
     * @param page - the page that has been updated
     * @param uri - the URI of the updated page.
     * @throws IOException
     */
    @Override
    public void onPageUpdated(ApiDatasetLandingPage page, String uri) throws IOException {

        Dataset dataset = new Dataset();
        dataset.setId(page.getapiDatasetId());

        try {
            datasetAPIClient.createDataset(page.getapiDatasetId(), dataset);
        } catch (DatasetAPIException e) {
            throw new RuntimeException(e);
        }
    }
}
