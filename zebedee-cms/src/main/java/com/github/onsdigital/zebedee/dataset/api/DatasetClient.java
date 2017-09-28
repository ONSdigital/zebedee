package com.github.onsdigital.zebedee.dataset.api;

import java.io.IOException;

public interface DatasetClient {

    /**
     * Get the dataset for the given dataset ID.
     *
     * @param datasetID
     * @return
     */
    Dataset getDataset(String datasetID) throws IOException;

    /**
     * Get the instance for the given instance ID.
     *
     * @param instanceID
     * @return
     */
    Instance getInstance(String instanceID) throws IOException;
}
