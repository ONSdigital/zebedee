package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.base.Page;
import dp.api.dataset.DatasetAPIClient;

import java.io.IOException;

public class DatasetLandingPageCreationHook implements PageUpdateHook {

    private DatasetAPIClient datasetAPIClient;

    public DatasetLandingPageCreationHook(DatasetAPIClient datasetAPIClient) {
        this.datasetAPIClient = datasetAPIClient;
    }

    @Override
    public void OnPageCreate(Page page, String uri) throws IOException {

        //datasetAPIClient.
    }
}
