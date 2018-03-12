package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.ApiDatasetLandingPage;
import dp.api.dataset.DatasetClient;
import dp.api.dataset.exception.DatasetAPIException;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * A PageUpdateHook implementation for when an ApiDatasetLandingPage is deleted.
 */
public class APIDatasetLandingPageDeletionHook implements PageUpdateHook<ApiDatasetLandingPage> {

    private DatasetClient datasetAPIClient;

    /**
     * Create a new instance using the given dataset client.
     * @param datasetAPIClient
     */
    public APIDatasetLandingPageDeletionHook(DatasetClient datasetAPIClient) {
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

        try {
            datasetAPIClient.deleteDataset(page.getapiDatasetId());
        } catch (DatasetAPIException e) {
            logError(e, "failed to delete dataset in the dataset api").log();
            throw new RuntimeException(e);
        }
    }
}
